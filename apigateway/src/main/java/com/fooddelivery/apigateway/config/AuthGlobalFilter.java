package com.fooddelivery.apigateway.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.fooddelivery.apigateway.dto.AuthDto;

import reactor.core.publisher.Mono;

@Component
public class AuthGlobalFilter implements GlobalFilter, org.springframework.core.Ordered {

    private final WebClient.Builder webClientBuilder;
    private final AuthHeaderFactory _AuthFactory;

    @Autowired
    public AuthGlobalFilter(WebClient.Builder webClientBuilder, AuthHeaderFactory authFactory) {
        this.webClientBuilder = webClientBuilder;
        this._AuthFactory = authFactory;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        if (path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return unauthorized(exchange);
        }

        String base64Credentials = authHeader.substring("Basic ".length()).trim();
        byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

        String[] parts = decodedString.split(":", 2);
        String username = parts[0];
        String password = parts.length > 1 ? parts[1] : "";

        AuthDto request = new AuthDto(username, password);
        WebClient client = webClientBuilder.build();
        Mono<ResponseEntity<Void>> responseMono = client.post()
                .uri("lb://authservice/auth/authenticate")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity();

        return responseMono.flatMap(response -> {
            if (response.getStatusCode().is2xxSuccessful()) {
                ServerHttpRequest mutatedRequest = exchange.getRequest()
                        .mutate()
                        .headers(httpHeaders -> httpHeaders.remove(HttpHeaders.AUTHORIZATION))
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } else {
                return unauthorized(exchange);
            }
        }).onErrorResume(ex -> unauthorized(exchange));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}