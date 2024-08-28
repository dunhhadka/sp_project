package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Builder
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferenceInfo {
    @Min(1)
    private int number;
    @Min(1)
    private int orderNumber;
    @NotBlank
    @Size(max = 50)
    private String name;
    @NotBlank
    @Size(max = 32)
    private String token;
}
