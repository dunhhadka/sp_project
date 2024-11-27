package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderReturnDto;

import java.util.List;

public interface OrderReturnDao {
    List<OrderReturnDto> getByOrderId(int storeId, int orderId);
}

