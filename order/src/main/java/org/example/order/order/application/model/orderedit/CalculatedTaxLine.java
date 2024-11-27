package org.example.order.order.application.model.orderedit;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CalculatedTaxLine {
    private String title;
    private BigDecimal price;
    private BigDecimal rate;
    private BigDecimal ratePercentage;
    private boolean custom;
}
