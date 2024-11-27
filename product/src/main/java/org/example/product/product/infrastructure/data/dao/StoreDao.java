package org.example.product.product.infrastructure.data.dao;

import org.example.product.product.infrastructure.data.dto.StoreDto;

public interface StoreDao {
    StoreDto findById(int id);
}
