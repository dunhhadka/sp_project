package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.FulfillmentOrderDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcFulfillmentOrderDao implements FulfillmentOrderDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<FulfillmentOrderDto> getByOrderId(int storeId, int orderId) {
        return jdbcTemplate.query(
                "SELECT * FROM fulfillment_orders WHERE store_id = :storeId AND order_id = :orderId",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("orderId", orderId),
                BeanPropertyRowMapper.newInstance(FulfillmentOrderDto.class)
        );
    }
}
