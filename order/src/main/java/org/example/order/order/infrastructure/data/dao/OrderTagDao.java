package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderTagDto;

import java.util.List;

public interface OrderTagDao {
    List<OrderTagDto> getByOrderIds(int storeId, List<Integer> orderIds);
}
