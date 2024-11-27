package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
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
public class ProductPricingInfo {
    @Builder.Default
    private BigDecimal priceMax = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal priceMin = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal compareAtPriceMax = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal compareAtPriceMin = BigDecimal.ZERO;
    @Builder.Default
    private boolean priceVaries = false; // có price hay không
    @Builder.Default
    private boolean compareAtPriceVaries = false; // có sự khác nhau giữa các variant hay không
}
