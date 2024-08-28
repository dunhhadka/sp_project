package org.example.order.order.domain.order.persistence;

import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;

public interface OrderRepository {
    void save(Order order);

    Order findById(OrderId id);
}
