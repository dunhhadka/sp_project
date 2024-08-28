package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.TaxLineDto;

import java.util.List;

public interface TaxLineDao {
    List<TaxLineDto> getByOrderIds(int storeId, List<Integer> orderIds);

}
