package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderEditDiscountApplicationDto;

import java.util.List;

public interface OrderEditDiscountApplicationDao {
    List<OrderEditDiscountApplicationDto> getByEditingId(int storeId, int editingId);
}
