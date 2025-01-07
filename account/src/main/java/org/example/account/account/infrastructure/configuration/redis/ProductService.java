package org.example.account.account.infrastructure.configuration.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private int id;
        private String name;
    }

    @Cacheable(cacheNames = "products", key = "#id")
    public Product getById(int id) {
        return productRepository.findById(id);
    }
}
