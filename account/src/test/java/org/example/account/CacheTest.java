package org.example.account;

import org.example.account.account.infrastructure.configuration.redis.ProductRepository;
import org.example.account.account.infrastructure.configuration.redis.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.*;

@SpringBootTest
public class CacheTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {

    }

    @Test
    void test() {
        int productId1 = 1;
        var product = new ProductService.Product(productId1, "product 1");
        when(productRepository.findById(productId1)).thenReturn(product);

        var product1 = productService.getById(productId1);

        var product2 = productService.getById(productId1);

        verify(productRepository, times(1)).findById(productId1);
    }
}
