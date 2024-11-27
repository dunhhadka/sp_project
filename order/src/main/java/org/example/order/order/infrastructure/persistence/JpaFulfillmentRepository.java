package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.fulfillment.model.FulfillmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaFulfillmentRepository implements FulfillmentRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<Fulfillment> getByOrderId(int storeId, int orderId) {
        return null;
    }

    @Override
    public void save(Fulfillment fulfillment) {
        if (fulfillment.isNew()) {
            entityManager.persist(fulfillment);
        } else {
            entityManager.merge(fulfillment);
        }
        entityManager.flush();
    }
}
