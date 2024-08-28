package org.example.order.order.domain.fulfillmentorder.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantInfo {
    private Integer productId;
    private String variantTitle;
    private String title;
    private String sku;
    private Float grams;
    private String vendor;
    private String image;
    private BigDecimal price;
    private BigDecimal discountedUnitPrice;
}
