package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.order.request.OrderFilterRequest;
import org.example.order.order.infrastructure.data.dto.OrderDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class JdbcOrderDao implements OrderDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final Set<String> _filterableStatus = Set.of("open", "closed", "cancelled", "deleted");

    @Override
    public OrderDto getByReference(int storeId, String reference) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT * FROM orders 
                        WHERE store_id = :storeId AND reference = :reference
                        AND status IN ('open', 'closed', 'cancelled')
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("reference", reference),
                BeanPropertyRowMapper.newInstance(OrderDto.class)
        );
    }

    @Override
    public List<OrderDto> filter(Integer storeId, OrderFilterRequest request) {
        if (isStatusNotFilterable(request.getStatus())) return List.of();
        return jdbcTemplate.query(
                OrderSql.INSTANCE.filter(List.of(), request),
                generateParamaterForFilter(storeId, List.of(), request),
                BeanPropertyRowMapper.newInstance(OrderDto.class)
        );
    }

    private MapSqlParameterSource generateParamaterForFilter(Integer storeId, List<Integer> locationIds, OrderFilterRequest request) {
        var paramSource = new MapSqlParameterSource().addValue("storeId", storeId);
        if (!CollectionUtils.isEmpty(locationIds)) {
            paramSource.addValue("locationIds", locationIds);
        }
        return paramSource
                .addValue("ids", request.getIds())
                .addValue("query", request.getQuery())
                .addValue("status", request.getStatus())
                .addValue("financialStatus", request.getFinancialStatus())
                .addValue("fulfillmentStatus", request.getFulfillmentStatus())
                .addValue("tag", request.getTag())
                .addValue("customerId", request.getCustomerId())
                .addValue("createdOnMin", JdbcUtils.bindTimeStamp(request.getCreatedOnMin()))
                .addValue("createdOnMax", JdbcUtils.bindTimeStamp(request.getCreatedOnMax()))
                .addValue("modifiedOnMin", JdbcUtils.bindTimeStamp(request.getModifiedOnMin()))
                .addValue("modifiedOnMax", JdbcUtils.bindTimeStamp(request.getModifiedOnMax()))
                .addValue("processOnMin", JdbcUtils.bindTimeStamp(request.getProcessOnMin()))
                .addValue("processOnMax", JdbcUtils.bindTimeStamp(request.getProcessOnMax()))
                .addValue("pageSize", request.getLimit())
                .addValue("pageIndex", request.getPage());
    }

    private boolean isStatusNotFilterable(String status) {
        return status != null && !_filterableStatus.contains(status);
    }
}
