package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VariantPricingInfo extends ValueObject<VariantPricingInfo> {
    @NotNull
    @Builder.Default
    @DecimalMin(value = "0")
    @DecimalMax(value = "1000000000")
    private BigDecimal price = BigDecimal.ZERO;

    @DecimalMin(value = "0")
    @DecimalMax(value = "1000000000")
    private BigDecimal compareAtPrice;

    private boolean taxable;
}
