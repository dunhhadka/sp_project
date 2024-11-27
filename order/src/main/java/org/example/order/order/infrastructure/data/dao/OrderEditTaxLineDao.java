package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderEditTaxLineDto;

import java.util.List;

public interface OrderEditTaxLineDao {
    List<OrderEditTaxLineDto> getByEditingId(int storeId, int editingId);
}
