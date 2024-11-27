package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderStagedChangeDto;

import java.util.List;

public interface OrderStagedChangeDao {
    List<OrderStagedChangeDto> getByEditingId(int storeId, int editingId);
}
