package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.domain.orderedit.persistence.OrderEditRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaOrderEditRepository implements OrderEditRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public void save(OrderEdit orderEdit) {
        boolean isNew = orderEdit.isNew();
        if (isNew) entityManager.persist(orderEdit);
        else entityManager.merge(orderEdit);
        entityManager.flush();
    }

    @Override
    public OrderEdit findById(OrderEditId id) {
        var orderEdit = entityManager.find(OrderEdit.class, id);
        return orderEdit;
    }
}
