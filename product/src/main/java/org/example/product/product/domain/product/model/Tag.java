package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

@Getter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    @Size(max = 255)
    private String name;
    private String alias;
}
