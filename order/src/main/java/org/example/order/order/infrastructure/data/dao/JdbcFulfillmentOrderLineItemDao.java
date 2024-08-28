package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.FulfillmentOrderLineItemDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcFulfillmentOrderLineItemDao implements FulfillmentOrderLineItemDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<FulfillmentOrderLineItemDto> getByFulfillmentOrderIds(int storeId, List<Long> ffoIds) {
        return jdbcTemplate.query(
                "SELECT * FROM fulfillment_order_line_items WHERE storeId = :storeId AND fulfillment_order_id IN (:ffoIds)",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("ffoIds", ffoIds),
                BeanPropertyRowMapper.newInstance(FulfillmentOrderLineItemDto.class)
        );
    }
}
