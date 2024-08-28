package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.LineItemDto;

import java.util.List;

public interface LineItemDao {
    List<LineItemDto> getByOrderIds(int storeId, List<Integer> orderIds);
}
