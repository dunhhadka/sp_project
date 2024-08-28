package org.example.product.product.domain.product.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.product.product.domain.product.model.Product;
import org.example.product.product.domain.product.model.ProductId;
import org.example.product.product.domain.product.model.ProductIdGenerator;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaProductRepository implements ProductRepository {

    @PersistenceContext
    private final EntityManager entityManager;
    private final ProductIdGenerator idGenerator;

    @Override
    public Product findById(ProductId id) {
        var product = entityManager.find(Product.class, id);
        if (product != null) product.setIdGenerator(idGenerator);
        return product;
    }
}
