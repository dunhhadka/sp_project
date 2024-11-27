package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.infrastructure.data.dto.RefundTaxLineDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcRefundTaxLineDao implements RefundTaxLineDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<RefundTaxLineDto> getByStoreIdAndOrderIds(int storeId, Collection<Integer> orderIds) {
        if (storeId > 0 && CollectionUtils.isNotEmpty(orderIds)) {
            return jdbcTemplate.query(
                    "SELECT * FROM refund_tax_lines WHERE store_id = :storeId AND order_id IN (:orderIds)",
                    new MapSqlParameterSource()
                            .addValue("storeId", storeId)
                            .addValue("orderIds", orderIds),
                    BeanPropertyRowMapper.newInstance(RefundTaxLineDto.class));
        }
        return new ArrayList<>(0);
    }
}
