package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.DiscountCodeDto;

import java.util.List;

public interface OrderDiscountDao {
    List<DiscountCodeDto> getByOrderIds(int storeId, List<Integer> orderIds);

}
