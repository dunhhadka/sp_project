package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CombinationDto {
    private int storeId;
    private int orderId;
    private int id;

    private long variantId;
    private long productId;

    private BigDecimal price;
    private BigDecimal quantity;

    private String title;
    private String variantTitle;
    private String name;

    private String sku;
    private String vendor;
    private String unit;
    private String itemUnit;
}
