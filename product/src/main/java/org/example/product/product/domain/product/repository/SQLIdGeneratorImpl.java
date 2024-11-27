package org.example.product.product.domain.product.repository;

import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;

@Repository
public class SQLIdGeneratorImpl implements ProductIdGenerator {
    @Override
    public Deque<Integer> generateImageIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public int generateProductId() {
        return 0;
    }

    @Override
    public int generateVariantId() {
        return 0;
    }

    @Override
    public int generateInventoryItemId() {
        return 0;
    }

    @Override
    public int generateInventoryLevelId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateVariantIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public Deque<Integer> generateInventoryItemIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public Deque<Integer> generateInventoryLevelIds(int size) {
        return new ArrayDeque<>();
    }
}
