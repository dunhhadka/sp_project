package org.example.order.order.domain.fulfillmentorder.model;

import org.springframework.stereotype.Repository;

@Repository
public class SequenceFulfillmentGenerator implements FulfillmentOrderIdGenerator {
    @Override
    public int generateFulfillmentOrderId() {
        return 0;
    }

    @Override
    public int generateFulfillmentOrderLineItemId() {
        return 0;
    }
}
