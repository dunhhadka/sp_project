package org.example.product.product.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.product.product.infrastructure.data.dto.StoreDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StoreDaoImpl implements StoreDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;


    @Override
    public StoreDto findById(int id) {
        var results = jdbcTemplate.query(
                "SELECT * FROM stores WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", id),
                BeanPropertyRowMapper.newInstance(StoreDto.class));

        return CollectionUtils.isEmpty(results) ? null : results.get(0);
    }
}
