package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderTagDto {
    private int storeId;
    private int orderId;
    private String value;
}
