package org.example.order.order.application.model.draftorder.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class DraftPropertyRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 250)
    private String value;
}
