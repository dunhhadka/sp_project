package org.example.order.order.application.service.fulfillmentorder;

import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;

public record MovedFulfillmentOrderEvent(
        FulfillmentOrderId originalFulfillmentOrderId,
        FulfillmentOrderId movedFulfillmentOrderId, Long originalLocationId,
        Long newLocationId) {
}
