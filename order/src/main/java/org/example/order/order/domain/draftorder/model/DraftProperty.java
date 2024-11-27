package org.example.order.order.domain.draftorder.model;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.Size;

@Getter
@Builder
public class DraftProperty {
    @Size(max = 250)
    private String name;

    @Size(max = 250)
    private String value;
}
