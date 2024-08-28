package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.DiscountApplicationDto;

import java.util.List;

public interface DiscountApplicationDao {
    List<DiscountApplicationDto> getByOrderIds(int storeId, List<Integer> orderIds);

}
