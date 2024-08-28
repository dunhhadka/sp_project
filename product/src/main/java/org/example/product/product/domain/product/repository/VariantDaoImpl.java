package org.example.product.product.domain.product.repository;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class VariantDaoImpl implements VariantDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public CompletableFuture<List<VariantDto>> getByProductIds(int storeId, List<Integer> productIds) {
        if (CollectionUtils.isEmpty(productIds)) return CompletableFuture.completedFuture(List.of());

        return CompletableFuture.supplyAsync(() ->
                jdbcTemplate.query("SELECT * FROM ProductVariants WHERE StoreId = :storeId AND ProductId IN (:productIds)",
                        new MapSqlParameterSource()
                                .addValue("storeId", storeId)
                                .addValue("productIds", productIds),
                        BeanPropertyRowMapper.newInstance(VariantDto.class)
                )
        );
    }
}
