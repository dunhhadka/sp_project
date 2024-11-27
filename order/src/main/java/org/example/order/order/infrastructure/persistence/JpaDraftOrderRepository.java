package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.draftorder.model.DraftOrder;
import org.example.order.order.domain.draftorder.persistence.DraftOrderRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaDraftOrderRepository implements DraftOrderRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public void store(DraftOrder draftOrder) {
        var isNew = draftOrder.isNew();
        if (isNew) {
            entityManager.persist(draftOrder);
        } else {
            entityManager.merge(draftOrder);
        }
        entityManager.flush();
    }
}
