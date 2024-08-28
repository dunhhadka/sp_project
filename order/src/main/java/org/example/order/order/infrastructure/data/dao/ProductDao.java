package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.application.model.order.context.Product;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;

import java.util.List;

public interface ProductDao {
    List<ProductDto> findProductByListIds(int storeId, List<Integer> productIds);

    List<VariantDto> findVariantByListIds(int storeId, List<Integer> variantIds);

    List<Product> getByIds(List<Integer> productIds);

}
