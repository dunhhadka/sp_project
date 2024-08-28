package org.example.product.product.domain.product.repository;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.product.product.domain.product.dto.ProductDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductDaoImpl implements ProductDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<ProductDto> getByIds(int storeId, List<Integer> productIds) {
        if (CollectionUtils.isEmpty(productIds)) return List.of();

        return jdbcTemplate.query("SELECT * FROM Products WHERE StoreId = :storeId ANd Id IN (:productIds)",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("productIds", productIds),
                BeanPropertyRowMapper.newInstance(ProductDto.class)
        );
    }
}
