package org.example.product.product.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.example.product.product.application.model.ProductCreateRequest;
import org.example.product.product.application.model.ProductResponse;
import org.example.product.product.application.service.ProductWriteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductWriteService productWriteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse productCreate(@RequestBody @Valid ProductCreateRequest request) throws ExecutionException, InterruptedException, IOException {
        Integer storeId = 1;
        var productId = productWriteService.createProduct(request, storeId);
        return null;
    }
}
