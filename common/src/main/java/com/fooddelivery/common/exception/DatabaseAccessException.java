package com.fooddelivery.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A database operation failed. The message and cause are only logged; clients receive a generic
 * message so SQL and schema details never leak.
 */
public class DatabaseAccessException extends ApiException {

    public DatabaseAccessException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    public DatabaseAccessException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
