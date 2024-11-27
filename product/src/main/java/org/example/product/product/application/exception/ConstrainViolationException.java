package org.example.product.product.application.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConstrainViolationException extends RuntimeException {
    private String code;
    private String message;
}
