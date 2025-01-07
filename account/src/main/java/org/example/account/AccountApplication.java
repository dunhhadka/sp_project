package org.example.account;

import lombok.RequiredArgsConstructor;
import org.example.account.account.infrastructure.configuration.redis.ProductService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RequiredArgsConstructor
@RestController
public class AccountApplication implements CommandLineRunner {

    private final ProductService productService;

    public static void main(String[] args) {
        SpringApplication.run(AccountApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
    }

    @GetMapping("/{id}")
    public ProductService.Product getProduct(@PathVariable int id) {
        return productService.getById(id);
    }

}
