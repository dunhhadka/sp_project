package org.example.order.order.application.utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class TaxSettingValue {
    private BigDecimal rate;
    private String title = "Tax";
    private Integer productId;

    private TaxType taxType;

    public enum TaxType {
        line_item, shipping
    }
}
