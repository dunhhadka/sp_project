package org.example.product.product.domain.product.model;

import java.util.Deque;

public interface ProductIdGenerator {
    Deque<Integer> generateImageIds(long imageCount);

    Integer generateVariantId();

    Deque<Integer> generateVariantIds(int size);

    Integer generateInventoryItemId();

    Deque<Integer> generateInventoryItemIds(int size);

    int generateProductId();
}
