package org.example.product.product.domain.product.model;

import org.springframework.stereotype.Repository;

import java.util.Deque;

@Repository
public class SqlSequenceGenerator implements ProductIdGenerator {
    @Override
    public Deque<Integer> generateImageIds(long imageCount) {
        return null;
    }

    @Override
    public Integer generateVariantId() {
        return null;
    }

    @Override
    public Deque<Integer> generateVariantIds(int size) {
        return null;
    }

    @Override
    public Integer generateInventoryItemId() {
        return null;
    }

    @Override
    public Deque<Integer> generateInventoryItemIds(int size) {
        return null;
    }

    @Override
    public int generateProductId() {
        return 0;
    }
}
