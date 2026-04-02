package com.copap.config;

import com.copap.auth.AuthTokenRepository;
import com.copap.auth.UserRepository;
import com.copap.security.TokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(AuthTokenRepository authTokenRepository,
                          UserRepository userRepository) {
        this.authTokenRepository = authTokenRepository;
        this.userRepository = userRepository;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(authTokenRepository, userRepository);
    }

    /**
     * CORS must be configured here at the Spring Security level, not only in
     * WebMvcConfigurer. Spring Security's filter chain runs BEFORE Spring MVC,
     * so preflight OPTIONS requests get blocked by auth checks before
     * WebMvcConfigurer ever has a chance to add CORS headers.
     *
     * With .cors(Customizer.withDefaults()), Spring Security's CorsFilter runs
     * first, responds to preflight correctly, and auth checks are skipped for
     * OPTIONS requests.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS must come first — handles preflight OPTIONS before any auth check
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/products", "/products/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                // Return 401 for unauthenticated requests so the frontend
                // can distinguish "no valid token" from "token valid but forbidden"
                .authenticationEntryPoint((request, response, e) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            );

        return http.build();
    }
}
