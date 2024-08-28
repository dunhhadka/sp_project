package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VariantPricingInfo {
    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "10000000000")
    private BigDecimal price;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "10000000000")
    private BigDecimal compareAtPrice;

    private boolean taxable;
}
