package org.example.order.order.application.model.fulfillmentorder.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;

@Getter
@AllArgsConstructor
public class FulfillmentOrderMovedResponse {
    private FulfillmentOrder originalFulfillmentOrder;
    private FulfillmentOrder movedFulfillmentOrder;
}
