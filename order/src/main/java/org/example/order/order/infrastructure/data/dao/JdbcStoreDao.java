package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.StoreDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcStoreDao implements StoreDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public StoreDto findById(int id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM stores WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", id),
                BeanPropertyRowMapper.newInstance(StoreDto.class));
    }
}
