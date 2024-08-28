package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.DiscountAllocationDto;

import java.util.List;

public interface DiscountAllocationDao {
    List<DiscountAllocationDto> getByOrderIds(int storeId, List<Integer> orderIds);

}
