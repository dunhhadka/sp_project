package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.FulfillmentOrderDto;

import java.util.List;

public interface FulfillmentOrderDao {
    List<FulfillmentOrderDto> getByOrderId(int storeId, int orderId);
}
