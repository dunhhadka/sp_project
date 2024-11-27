package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.RefundTaxLineDto;

import java.util.Collection;
import java.util.List;

public interface RefundTaxLineDao {
    List<RefundTaxLineDto> getByStoreIdAndOrderIds(int storeId, Collection<Integer> orderIds);
}
