package org.example.product.product.domain.product.repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ImageDao {
    CompletableFuture<List<ImageDto>> getByProductIds(int storeId, List<Integer> productIds);
}
