package org.example.product.product.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.product.product.domain.product.model.Product;
import org.example.product.product.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaProductRepository implements ProductRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public void store(Product product) {
        var isNew = product.isNew();
        if (isNew) entityManager.persist(product);
        else entityManager.merge(product);
    }
}
