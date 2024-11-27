package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderEditDto;

public interface OrderEditDao {
    OrderEditDto getById(int storeId, int id);
}
