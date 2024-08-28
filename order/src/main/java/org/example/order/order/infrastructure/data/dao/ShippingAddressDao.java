package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.ShippingAddressDto;

import java.util.List;

public interface ShippingAddressDao {
    List<ShippingAddressDto> getByOrderIds(int storeId, List<Integer> orderIds);
}
