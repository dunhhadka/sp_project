package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderIdGenerator;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaFulfillmentOrderRepository implements FulfillmentOrderRepository {

    @PersistenceContext
    private final EntityManager entityManager;
    private final FulfillmentOrderIdGenerator idGenerator;

    @Override
    public void save(FulfillmentOrder fulfillmentOrder) {
        if (fulfillmentOrder.isNew()) {
            entityManager.persist(fulfillmentOrder);
        } else {
            entityManager.merge(fulfillmentOrder);
        }

        entityManager.flush();
    }

    @Override
    public List<FulfillmentOrder> findByOrderId(int storeId, int orderId) {
        return null;
    }

    @Override
    public Optional<FulfillmentOrder> findById(FulfillmentOrderId id) {
        var fulfillmentOrder = entityManager.find(FulfillmentOrder.class, id);
        if (fulfillmentOrder != null) {
            fulfillmentOrder.setIdGenerator(idGenerator);
        }
        return Optional.ofNullable(fulfillmentOrder);
    }
}
