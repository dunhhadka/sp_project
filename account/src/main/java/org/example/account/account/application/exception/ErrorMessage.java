package org.example.account.account.application.exception;

import java.util.*;

public class ErrorMessage {

    private String message;

    Map<String, List<String>> errors;

    private List<UserError> userErrors;

    public static ErrorMessageBuilder builder() {
        return new ErrorMessageBuilder();
    }

    public List<UserError> getUserErrors() {
        return Collections.unmodifiableList(userErrors);
    }

    public static class ErrorMessageBuilder {
        private Map<String, List<String>> errors;
        private List<UserError> userErrors;

        private String message;

        public ErrorMessageBuilder addError(UserError userError) {
            if (userErrors == null) {
                userErrors = new ArrayList<>();
            }
            userErrors.add(userError);
            return this;
        }

        public ErrorMessage build() {
            var error = new ErrorMessage();
            error.errors = errors;
            error.userErrors = userErrors;
            error.message = message;
            return error;
        }

        public ErrorMessageBuilder addError(String key, String message) {
            if (errors == null) {
                errors = new HashMap<>();
            }
            errors.putIfAbsent(key, new ArrayList<>());
            errors.get(key).add(message);
            return this;
        }
    }
}
