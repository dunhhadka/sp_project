package org.example.order.application.service.order;

import lombok.extern.slf4j.Slf4j;
import org.example.order.order.infrastructure.configuration.RedisOrderCacheConfig;
import org.example.order.order.infrastructure.configuration.cache.CustomRedisCacheConfiguration;
import org.example.order.order.infrastructure.configuration.cache.ProductRepository;
import org.example.order.order.infrastructure.configuration.cache.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@EnableCaching
@Slf4j
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@Import({RedisOrderCacheConfig.class, CustomRedisCacheConfiguration.class, ProductService.class, ProductRepository.class})
public class SpringCacheTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @BeforeEach
    public void setUp() {
        Arrays.stream(applicationContext.getBeanDefinitionNames()).forEach(log::info);
    }

    @Test
    public void test() {
        int productId = 11;
        ProductService.Product mockProduct = new ProductService.Product(productId, "Mock Product");
        when(productRepository.findById(productId)).thenReturn(mockProduct);

        ProductService.Product product1 = productService.getProductById(productId);

        ProductService.Product product2 = productService.getProductById(productId);

        assertEquals(mockProduct, product1);
        assertEquals(mockProduct, product2);
        verify(productRepository, times(1)).findById(productId);
    }
}
