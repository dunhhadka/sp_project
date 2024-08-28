package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.DiscountCodeDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcDiscountCodeDao implements OrderDiscountDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<DiscountCodeDto> getByOrderIds(int storeId, List<Integer> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) return List.of();
        return jdbcTemplate.query(
                "SELECT * FROM order_discounts WHERE store_id = :storeId AND order_id IN (:orderIds)",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("orderIds", orderIds),
                BeanPropertyRowMapper.newInstance(DiscountCodeDto.class)
        );
    }
}
