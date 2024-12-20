package org.example.order.order.application.exception;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.*;

@JsonSerialize(using = ErrorMessage.ErrorMessageSerialize.class)
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

    public static class ErrorMessageSerialize extends JsonSerializer<ErrorMessage> {

        @Override
        public void serialize(ErrorMessage errorMessage, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        }
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

        public ErrorMessageBuilder addErrors(List<UserError> userErrors) {
            if (this.userErrors == null) this.userErrors = new ArrayList<>();
            this.userErrors.addAll(userErrors);
            return this;
        }
    }
}
