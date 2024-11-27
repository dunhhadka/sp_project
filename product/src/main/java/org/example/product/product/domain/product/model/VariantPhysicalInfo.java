package org.example.product.product.domain.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;

import javax.validation.constraints.Size;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VariantPhysicalInfo extends ValueObject<VariantPhysicalInfo> {

    @Builder.Default
    private boolean requireShipping = true;

    @Builder.Default
    private double weight = 0;

    @Size(max = 20)
    private String weightUnit = "kg";

    @Size(max = 50)
    private String unit;

    public int weightInGram() {
        double value = switch (weightUnit) {
            case "kg" -> weight * 1000;
            case "lb" -> weight / 0.0022046;
            case "oz" -> weight / 0.035274;
            default -> weight;
        };
        return (int) value;
    }

}
