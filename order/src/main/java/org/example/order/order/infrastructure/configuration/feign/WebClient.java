package org.example.order.order.infrastructure.configuration.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "product-service", url = "http://localhost:8080", configuration = LoggingFeignConfig.class)
public interface WebClient {

    @GetMapping("/admin/products")
    String get();
}
