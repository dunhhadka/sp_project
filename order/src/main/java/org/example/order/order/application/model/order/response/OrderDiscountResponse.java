package org.example.order.order.application.model.order.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderDiscountResponse {
    private String code;
    private BigDecimal amount;
    private String type;
    private Boolean custom;
}
