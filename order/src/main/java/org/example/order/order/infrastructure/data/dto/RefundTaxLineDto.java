package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class RefundTaxLineDto {
    private int id;
    private int storeId;
    private int orderId;
    private int taxLineId;
    private BigDecimal amount;
    private Instant createdAt;
}
