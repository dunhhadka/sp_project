package org.example.order.order.application.model.order.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CombinationLineComponentResponse {
    private Long variantId;
    private Long productId;
    private int grams;
    private boolean taxable;
    private boolean productExists;
    private BigDecimal quantity;

    private String title;
    private String variantTitle;
    private String sku;
    private String unit;

    private ImageResponse image;
}
