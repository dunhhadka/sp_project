package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.model.OrderIdGenerator;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {

    private final OrderIdGenerator orderIdGenerator;

    private final EntityManager entityManager;

    @Override
    public void save(Order order) {

        if (order.isNew()) entityManager.persist(order);
        else entityManager.merge(order);

        entityManager.flush();
    }

    @Override
    public Order findById(OrderId id) {
        var order = entityManager.find(Order.class, id);
        if (order != null) {
            order.setIdGenerator(orderIdGenerator);
        }
        return order;
    }
}
