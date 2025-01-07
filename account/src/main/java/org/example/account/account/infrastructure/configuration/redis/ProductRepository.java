package org.example.account.account.infrastructure.configuration.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class ProductRepository {
    static List<ProductService.Product> products = List.of(
            new ProductService.Product(1, "product 1"),
            new ProductService.Product(2, "product 2"),
            new ProductService.Product(3, "product 2"),
            new ProductService.Product(4, "product 2")
    );

    public ProductService.Product findById(int id) {
        try {
            log.info("get to db {}", id);
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return products.stream().filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(IllegalAccessError::new);
    }
}
