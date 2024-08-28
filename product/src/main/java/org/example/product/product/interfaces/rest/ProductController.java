package org.example.product.product.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.product.product.application.model.request.ProductRequest;
import org.example.product.product.application.model.request.ProductVariantRequest;
import org.example.product.product.application.service.product.ProductWriteService;
import org.example.product.product.domain.product.model.ProductId;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductWriteService productWriteService;

    @PostMapping
    private void createProduct(@RequestBody @Valid ProductRequest request) throws ExecutionException, InterruptedException, IOException {
        productWriteService.create(request);
    }

    @PutMapping("/{id}")
    public void updateProduct(@RequestBody @Valid ProductRequest request, @PathVariable int id) throws IOException, ExecutionException, InterruptedException {
        var storeId = 1;
        var productId = new ProductId(storeId, id);
        productWriteService.update(productId, request);
    }

    @PostMapping("/{productId}")
    public void createProductVariant(@RequestBody @Valid ProductVariantRequest request, @PathVariable int productId) {
        productWriteService.createVariant(new ProductId(1, productId), request);
    }
}
