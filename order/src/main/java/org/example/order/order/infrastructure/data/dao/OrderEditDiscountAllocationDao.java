package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderEditDiscountAllocationDto;

import java.util.List;

public interface OrderEditDiscountAllocationDao {
    List<OrderEditDiscountAllocationDto> getByEditingId(int storeId, int editingId);
}
