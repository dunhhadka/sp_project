package org.example.order.order.application.model.order.request;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@Jacksonized
public class AdjustmentRequest {
    private long locationId;
    private String reason;
    private String referenceDocumentName;
    private String referenceDocumentUrl;
    private String referenceDocumentType;
    private long referenceRootId;
    private long referenceDocumentId;
    private List<InventoryTransactionLineItemRequest> lineItems;
    private Instant issueAt;
}
