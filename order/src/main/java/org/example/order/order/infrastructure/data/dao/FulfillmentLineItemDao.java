package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.FulfillmentLineItemDto;

import java.util.List;

public interface FulfillmentLineItemDao {
    List<FulfillmentLineItemDto> getByFulfillmentIds(int storeId, List<Integer> orderId, List<Integer> fulfillmentIds);
}

