package org.example.order.order.application.model.order.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DiscountAllocationResponse {
    private BigDecimal amount;
    private int discountApplicationIndex;
}
