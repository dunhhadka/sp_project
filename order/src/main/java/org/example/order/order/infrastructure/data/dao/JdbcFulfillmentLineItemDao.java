package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.FulfillmentLineItemDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcFulfillmentLineItemDao implements FulfillmentLineItemDao{
    @Override
    public List<FulfillmentLineItemDto> getByFulfillmentIds(int storeId, List<Integer> orderId, List<Integer> fulfillmentIds) {
        return null;
    }
}
