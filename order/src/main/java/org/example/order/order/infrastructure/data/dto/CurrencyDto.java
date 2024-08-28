package org.example.order.order.infrastructure.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrencyDto {
    private String name;
    private String code;
    private int currencyDecimalDigits;
}
