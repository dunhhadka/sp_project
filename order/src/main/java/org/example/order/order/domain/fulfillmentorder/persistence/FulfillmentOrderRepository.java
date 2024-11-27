package org.example.order.order.domain.fulfillmentorder.persistence;

import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;

import java.util.List;
import java.util.Optional;

public interface FulfillmentOrderRepository {

    void save(FulfillmentOrder fulfillmentOrder);

    List<FulfillmentOrder> findByOrderId(int storeId, int orderId);

    Optional<FulfillmentOrder> findById(FulfillmentOrderId id);
}
