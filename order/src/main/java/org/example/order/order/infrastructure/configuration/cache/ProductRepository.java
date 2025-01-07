package org.example.order.order.infrastructure.configuration.cache;

import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private static int count = 0;

    public ProductService.Product findById(int id) {
        String name = count + "123";
        return new ProductService.Product(count, "");
    }
}
