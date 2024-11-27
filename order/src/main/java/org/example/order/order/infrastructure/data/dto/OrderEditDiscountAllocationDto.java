package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderEditDiscountAllocationDto {
    private UUID id;
    private int storeId;
    private int editingId;

    private int applicationId;
    private BigDecimal allocatedAmount;
    private UUID lineItemId;

    private Instant updatedAt;
    private Integer version;
}
