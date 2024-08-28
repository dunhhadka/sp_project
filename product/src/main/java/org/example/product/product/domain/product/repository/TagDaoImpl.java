package org.example.product.product.domain.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class TagDaoImpl implements ProductTagDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public CompletableFuture<List<TagDto>> getByProductIds(int storeId, List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) return CompletableFuture.completedFuture(List.of());

        return CompletableFuture.supplyAsync(() ->
                jdbcTemplate.query("SELECT * FROM ProductTags WHERE StoreId = :storeId AND ProductId IN (:productIds)",
                        new MapSqlParameterSource()
                                .addValue("productIds", productIds)
                                .addValue("storeId", storeId),
                        BeanPropertyRowMapper.newInstance(TagDto.class)));
    }
}
