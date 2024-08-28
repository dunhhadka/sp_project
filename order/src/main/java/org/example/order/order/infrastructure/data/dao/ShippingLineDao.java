package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.ShippingLineDto;

import java.util.List;

public interface ShippingLineDao {
    List<ShippingLineDto> getByOrderIds(int storeId, List<Integer> orderIds);
}
