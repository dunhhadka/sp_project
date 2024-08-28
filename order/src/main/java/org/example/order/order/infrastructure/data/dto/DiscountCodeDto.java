package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.OrderDiscountCode;

import java.math.BigDecimal;

@Getter
@Setter
public class DiscountCodeDto {
    private int storeId;
    private int orderId;
    private int id;

    private String code;
    private BigDecimal amount;
    private OrderDiscountCode.ValueType type;

    private Boolean custom;

    private BigDecimal value;
}
