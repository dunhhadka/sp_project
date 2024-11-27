package org.example.product.product.domain.product.repository;

import java.util.Deque;

public interface ProductIdGenerator {
    Deque<Integer> generateImageIds(int size);

    int generateProductId();

    int generateVariantId();

    int generateInventoryItemId();

    int generateInventoryLevelId();

    Deque<Integer> generateVariantIds(int size);

    Deque<Integer> generateInventoryItemIds(int size);

    Deque<Integer> generateInventoryLevelIds(int size);
}
