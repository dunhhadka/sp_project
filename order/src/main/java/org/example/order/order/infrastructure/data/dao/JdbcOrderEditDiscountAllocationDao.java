package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.OrderEditDiscountAllocationDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcOrderEditDiscountAllocationDao implements OrderEditDiscountAllocationDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<OrderEditDiscountAllocationDto> getByEditingId(int storeId, int editingId) {
        return jdbcTemplate.query(
                "SELECT * FROM order_edit_discount_allocations WHERE store_id = :storeId AND editing_id = :editingId ORDER BY Id ASC",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("editingId", editingId),
                BeanPropertyRowMapper.newInstance(OrderEditDiscountAllocationDto.class)
        );
    }
}
