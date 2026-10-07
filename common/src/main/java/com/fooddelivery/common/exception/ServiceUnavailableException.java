package com.fooddelivery.common.exception;

import org.springframework.http.HttpStatus;

/** A downstream service could not be reached or answered with an error. The message is safe to show clients. */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
