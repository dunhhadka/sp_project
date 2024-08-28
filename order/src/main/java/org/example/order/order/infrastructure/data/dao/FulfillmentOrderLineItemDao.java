package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.FulfillmentOrderLineItemDto;

import java.util.List;

public interface FulfillmentOrderLineItemDao {
    List<FulfillmentOrderLineItemDto> getByFulfillmentOrderIds(int storeId, List<Long> ffoIds);
}
