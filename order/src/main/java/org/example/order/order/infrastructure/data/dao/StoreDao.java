package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.StoreDto;

public interface StoreDao {
    StoreDto findById(int id);
}
