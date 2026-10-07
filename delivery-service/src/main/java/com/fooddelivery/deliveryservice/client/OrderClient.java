package com.fooddelivery.deliveryservice.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.exception.ServiceUnavailableException;

/** Asks order-service whether an order exists. The caller's own token is forwarded, so its permissions apply. */
@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(RestClient orderServiceRestClient) {
        this.restClient = orderServiceRestClient;
    }

    public void assertOrderExists(long orderId) {
        try {
            restClient.get()
                    .uri("/api/orders/{id}", orderId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Order not found with id: " + orderId);
            }
            throw new ServiceUnavailableException("Order service is currently unavailable.", e);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Order service is currently unavailable.", e);
        }
    }
}
