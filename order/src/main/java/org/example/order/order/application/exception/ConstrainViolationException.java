package org.example.order.order.application.exception;

public class ConstrainViolationException extends RuntimeException {

    public ConstrainViolationException(String key, String message) {
        super(key + "_" + message);
    }
}
