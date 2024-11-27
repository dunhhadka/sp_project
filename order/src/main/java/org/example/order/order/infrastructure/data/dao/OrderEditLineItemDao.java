package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderEditLineItemDto;

import java.util.List;

public interface OrderEditLineItemDao {
    List<OrderEditLineItemDto> getByEditingId(int storeId, int editingId);
}
