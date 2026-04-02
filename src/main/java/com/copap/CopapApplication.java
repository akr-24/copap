package com.copap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableAsync
@EnableRetry
public class CopapApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopapApplication.class, args);
    }
}
