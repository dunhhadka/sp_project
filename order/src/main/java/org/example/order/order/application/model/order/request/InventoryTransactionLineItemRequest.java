package org.example.order.order.application.model.order.request;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class InventoryTransactionLineItemRequest {
    private long inventoryItemId;
    private List<InventoryAdjustmentTransactionChangeRequest> changes;
}
