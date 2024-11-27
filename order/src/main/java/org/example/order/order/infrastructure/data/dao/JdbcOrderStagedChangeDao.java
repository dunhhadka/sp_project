package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.OrderStagedChangeDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcOrderStagedChangeDao implements OrderStagedChangeDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<OrderStagedChangeDto> getByEditingId(int storeId, int editingId) {
        return jdbcTemplate.query(
                "SELECT * FROM order_staged_changes WHERE store_id = :storeId AND editing_id = :editingId ORDER BY Id ASC",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("editingId", editingId),
                BeanPropertyRowMapper.newInstance(OrderStagedChangeDto.class)
        );
    }
}
