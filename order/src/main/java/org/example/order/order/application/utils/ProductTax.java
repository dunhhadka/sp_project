package org.example.order.order.application.utils;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductTax {
    private int productId;
    private BigDecimal taxRate;
    private String taxName;
}
