package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.CombinationDto;
import org.example.order.order.infrastructure.data.dto.TaxLineDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcTaxLineDao implements TaxLineDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<TaxLineDto> getByOrderIds(int storeId, List<Integer> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) return List.of();
        return jdbcTemplate.query(
                "SELECT * FROM tax_lines WHERE store_id = :storeId AND order_id IN (:orderIds)",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("orderIds", orderIds),
                BeanPropertyRowMapper.newInstance(TaxLineDto.class)
        );
    }
}