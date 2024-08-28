package org.example.product.product.domain.product.repository;

import org.example.product.product.domain.product.dto.ProductDto;

import java.util.List;

public interface ProductDao {
    List<ProductDto> getByIds(int storeId, List<Integer> productIds);
}
