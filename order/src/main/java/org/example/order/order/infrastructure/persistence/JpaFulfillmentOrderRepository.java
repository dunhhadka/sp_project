package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaFulfillmentOrderRepository implements FulfillmentOrderRepository {

    @PersistenceContext
    private final EntityManager entityManager;


    @Override
    public void save(FulfillmentOrder fulfillmentOrder) {
        if (fulfillmentOrder.isNew()) {
            entityManager.persist(fulfillmentOrder);
        } else {
            entityManager.merge(fulfillmentOrder);
        }

        entityManager.flush();
    }
}
