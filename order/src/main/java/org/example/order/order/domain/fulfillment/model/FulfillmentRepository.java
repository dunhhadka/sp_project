package org.example.order.order.domain.fulfillment.model;

import java.util.List;

public interface FulfillmentRepository {
    List<Fulfillment> getByOrderId(int storeId, int orderId);

    void save(Fulfillment fulfillment);
}
