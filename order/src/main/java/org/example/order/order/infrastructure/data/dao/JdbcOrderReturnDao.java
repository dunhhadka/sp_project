package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderReturnDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcOrderReturnDao implements OrderReturnDao{
    @Override
    public List<OrderReturnDto> getByOrderId(int storeId, int orderId) {
        return null;
    }
}
