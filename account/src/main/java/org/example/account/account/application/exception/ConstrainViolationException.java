package org.example.order.order.application.exception;

public class ConstrainViolationException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public ConstrainViolationException(String key, String message) {
        this.errorMessage = ErrorMessage.builder().addError(key, message).build();
    }

    public ConstrainViolationException(UserError userError) {
        this.errorMessage = ErrorMessage.builder().addError(userError).build();
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}
