package org.example.order.order.application.utils;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ShippingTax {
    private BigDecimal taxRate;
    private String taxName;
}
