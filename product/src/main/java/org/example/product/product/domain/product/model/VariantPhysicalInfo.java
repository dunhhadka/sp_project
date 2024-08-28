package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.product.application.annotation.StringInList;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VariantPhysicalInfo {
    @NotNull
    @Min(0)
    @Max(2000000)
    private Double weight;

    @StringInList(array = {"kg", "g"})
    private String weightUnit;

    @Size(max = 50)
    private String unit;
}
