package org.example.order.order.application.model.order.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TaxLineResponse {
    private BigDecimal rate;
    private String title;
    private BigDecimal price;
    private boolean custom;
}
