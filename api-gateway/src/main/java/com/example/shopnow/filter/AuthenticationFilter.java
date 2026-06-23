package com.example.shopnow.filter;

import com.example.shopnow.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/authenticate",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/webjars"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isSecured(request)) {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            authHeader = authHeader.substring(7);

            try {
                jwtUtil.validateToken(authHeader);
                String userId = jwtUtil.extractUserId(authHeader);
                System.out.println("Authenticated user ID: " + userId);

                // Mutate the request to add the user id to headers for downstream services
                request = exchange.getRequest()
                        .mutate()
                        .header("X-Auth-User-Id", userId)
                        .build();

                return chain.filter(exchange.mutate().request(request).build());

            } catch (Exception e) {
                System.out.println("Invalid access: " + e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }
        return chain.filter(exchange);
    }

    private boolean isSecured(ServerHttpRequest request) {
        if (request.getMethod().name().equals("OPTIONS")) {
            return false;
        }
        return OPEN_API_ENDPOINTS.stream()
                .noneMatch(uri -> request.getURI().getPath().contains(uri));
    }

    @Override
    public int getOrder() {
        return -1; // Highest precedence
    }
}
