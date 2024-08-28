package org.example.product.product.infrastructure.cache;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public KeyGenerator getProductByIdsKeyGenerator() {
        return ((target, method, params) -> {
            var storeId = (int) params[0];
            var productIds = (List<Integer>) params[1];
            return String.format("product.%s.%s", storeId, StringUtils.join(productIds, ","));
        });
    }
}
