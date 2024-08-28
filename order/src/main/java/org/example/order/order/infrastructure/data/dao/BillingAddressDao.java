package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.BillingAddressDto;

import java.util.List;

public interface BillingAddressDao {
    List<BillingAddressDto> getByOrderIds(int storeId, List<Integer> orderIds);
}
