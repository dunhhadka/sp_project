package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.application.model.order.request.OrderFilterRequest;
import org.example.order.order.infrastructure.data.dto.OrderDto;

import java.util.List;

public interface OrderDao {
    OrderDto getByReference(int storeId, String reference);

    List<OrderDto> filter(Integer storeId, OrderFilterRequest request);
}
