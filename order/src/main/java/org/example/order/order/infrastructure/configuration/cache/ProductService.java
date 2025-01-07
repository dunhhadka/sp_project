package org.example.order.order.infrastructure.configuration.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Product {
        private int id;
        private String name;
    }

    @Cacheable(value = "products", key = "#id")
    public Product getProductById(int id) {
        return productRepository.findById(id);
    }
}
