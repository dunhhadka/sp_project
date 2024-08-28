package org.example.order.order.domain.fulfillmentorder.persistence;

import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;

public interface FulfillmentOrderRepository {

    void save(FulfillmentOrder fulfillmentOrder);
}
