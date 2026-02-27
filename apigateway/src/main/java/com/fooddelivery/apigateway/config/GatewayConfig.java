package com.fooddelivery.apigateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

    @Autowired
    private AuthHeaderFactory _AuthFactory;

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service-route", r -> r.path("/auth/**")
                        .uri("lb://authservice"))

                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f.addRequestHeader(
                                HttpHeaders.AUTHORIZATION,
                                _AuthFactory.BuildAuthHeader("user-service")))
                        .uri("lb://user-service"))

                .route("delivery-service", r -> r.path("/api/deliveries/**", "/api/partners/**")
                        .filters(f -> f.addRequestHeader(
                                HttpHeaders.AUTHORIZATION,
                                _AuthFactory.BuildAuthHeader("delivery-service")))
                        .uri("lb://delivery-service"))
                        
                .route("orderservice-route", r -> r.path("/api/orders/**")
                        .filters(f -> f.addRequestHeader("X-API-GATEWAY-SECRET", _AuthFactory.getSharedSecret()))
                        .uri("lb://ORDERSERVICE"))

                .build();
    }
}