package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ShippingLineDto {
    private int storeId;
    private int orderId;
    private int id;

    private String code;
    private String title;
    private BigDecimal price;
}
