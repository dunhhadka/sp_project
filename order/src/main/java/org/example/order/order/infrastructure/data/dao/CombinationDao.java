package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.CombinationDto;

import java.util.List;

public interface CombinationDao {
    List<CombinationDto> getByOrderIds(int storeId, List<Integer> orderIds);
}
