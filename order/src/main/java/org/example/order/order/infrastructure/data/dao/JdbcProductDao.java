package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.order.context.Product;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcProductDao implements ProductDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<ProductDto> findProductByListIds(int storeId, List<Integer> productIds) {
        if (CollectionUtils.isEmpty(productIds)) return List.of();
        return jdbcTemplate.query(
                " SELECT * FROM products WHERE store_id = :storeId AND id IN (:productIds)",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("productIds", productIds),
                BeanPropertyRowMapper.newInstance(ProductDto.class)
        );
    }

    @Override
    public List<VariantDto> findVariantByListIds(int storeId, List<Integer> variantIds) {
        if (CollectionUtils.isEmpty(variantIds)) return List.of();
        return jdbcTemplate.query(
                " SELECT * FROM variants WHERE store_id = :storeId AND id IN (:variantIds)",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("variantIds", variantIds),
                BeanPropertyRowMapper.newInstance(VariantDto.class)
        );
    }

    @Override
    public List<Product> getByIds(List<Integer> productIds) {
        return List.of();
    }
}
