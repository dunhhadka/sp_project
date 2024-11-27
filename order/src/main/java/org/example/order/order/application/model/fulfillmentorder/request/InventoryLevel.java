package org.example.order.order.application.model.fulfillmentorder.request;


import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class InventoryLevel {
    private long id;
    private long storeId;
    private int inventoryItemId;
    private int locationId;
    private BigDecimal onHand = BigDecimal.ZERO;
    private BigDecimal available = BigDecimal.ZERO;
    private BigDecimal committed = BigDecimal.ZERO;
    private BigDecimal incoming = BigDecimal.ZERO;
    private Instant createdAt;
    private Instant updatedAt;
}
