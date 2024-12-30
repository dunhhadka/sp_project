package org.example.order.application.service.fulfillmentorder;


import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.service.fulfillmentorder.FulfillmentOrderSideEffectService;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
public class TestRefundFulfillmentOrderComparator {

    @Test
    void test_fulfillment_order_comparator() {
        FulfillmentOrder order1 = build(FulfillmentOrder.FulfillmentOrderStatus.closed, 1, 100);
        FulfillmentOrder order2 = build(FulfillmentOrder.FulfillmentOrderStatus.open, 2, 110);
        FulfillmentOrder order3 = build(FulfillmentOrder.FulfillmentOrderStatus.open, 5, 90);
        FulfillmentOrder order4 = build(FulfillmentOrder.FulfillmentOrderStatus.closed, 4, 150);

        var list = List.of(order1, order2, order3, order4);

        var sortResult = list.stream()
                .sorted(FulfillmentOrderSideEffectService.FULFILLMENT_ORDER_COMPARATOR)
                .toList();

    }

    FulfillmentOrder build(FulfillmentOrder.FulfillmentOrderStatus status, int id, int locationId) {
        return FulfillmentOrder.builder()
                .status(status)
                .id(new FulfillmentOrderId(1, id))
                .assignedLocationId((long) locationId)
                .build();
    }

}
