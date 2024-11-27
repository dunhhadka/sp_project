package org.example.account.account.application.exception;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@EqualsAndHashCode
public class UserError {
    private String code;
    private String message;
    @Builder.Default
    private List<String> fields = new ArrayList<>();
}
