package com.copap.api;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class CorsFilter extends Filter {

    @Override
    public void doFilter(HttpExchange exchange, Chain chain)
            throws IOException {

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        System.out.println("CorsFilter hit - Method: " + method + ", Path: " + path);
        
        exchange.getResponseHeaders().add(
                "Access-Control-Allow-Origin", "http://localhost:5173"
        );
        exchange.getResponseHeaders().add(
                "Access-Control-Allow-Headers",
                "Authorization, Content-Type, Idempotency-Key"
        );
        exchange.getResponseHeaders().add(
                "Access-Control-Allow-Methods",
                "GET, POST, OPTIONS, PUT, DELETE"
        );
        exchange.getResponseHeaders().add(
                "Access-Control-Allow-Credentials", "true"
        );

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            System.out.println("Handling OPTIONS preflight request for: " + path);
            exchange.sendResponseHeaders(204, 0);
            exchange.close();
            return;
        }

        chain.doFilter(exchange);
    }

    @Override
    public String description() {
        return "CORS filter";
    }
}