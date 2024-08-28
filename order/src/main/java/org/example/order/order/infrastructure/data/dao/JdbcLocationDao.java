package org.example.order.order.infrastructure.data.dao;


import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.Location;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcLocationDao implements LocationDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Location getById(int storeId, int id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM locations WHERE store_id = :storeId AND id = :id",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("id", id),
                BeanPropertyRowMapper.newInstance(Location.class)
        );
    }
}
