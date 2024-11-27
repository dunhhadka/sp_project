package org.example.order.order.application.model.order.request;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Getter
@Builder
@Jacksonized
public class InventoryAdjustmentTransactionChangeRequest {
    private BigDecimal value;
    private String valueType;
    private String changeType;
}
