package com.fooddelivery.deliveryservice.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fooddelivery.common.security.BearerTokenForwardingInterceptor;

@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient orderServiceRestClient(RestClient.Builder builder,
            @Value("${app.http-client.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${app.http-client.read-timeout-ms:5000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder
                .baseUrl("http://order-service")
                .requestFactory(requestFactory)
                .requestInterceptor(new BearerTokenForwardingInterceptor())
                .build();
    }
}
