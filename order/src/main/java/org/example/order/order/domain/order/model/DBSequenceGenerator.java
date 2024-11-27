package org.example.order.order.domain.order.model;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;

@Repository
@RequiredArgsConstructor
public class DBSequenceGenerator implements OrderIdGenerator {

    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_SEQUENCE_RANGE_QUERY = """
            declare @starting_range_out sql_variant
            EXEC sp_sequence_get_range @sequence_name = %s, @range_size = %d , @range_first_value = @starting_range_out OUTPUT
            SELECT CONVERT(int, @starting_range_out) as IntResult
            """;

    private int generateId(String sequenceName) {
        var result = jdbcTemplate.queryForObject("SELECT NEXT VALUE FOR " + sequenceName, Integer.class);
        return result == null ? 0 : result;
    }

    private Deque<Integer> generateIds(String sequenceName, int size) {
        if (size > 0) {
            var query = String.format(BASE_SEQUENCE_RANGE_QUERY, sequenceName, size);
            var startValue = jdbcTemplate.queryForObject(query, Integer.class);
            if (startValue != null) {
                var result = new ArrayDeque<Integer>(size);
                for (int i = 0; i < size; i++) {
                    result.add(startValue++);
                }
                return result;
            }
        }
        return new ArrayDeque<>();
    }

    @Override
    public int generateBillingAddressId() {
        return generateId("BillingAddressSequence");
    }

    @Override
    public int generateShippingAddressId() {
        return generateId("ShippingAddressSequence");
    }

    @Override
    public Deque<Integer> generateCombinationLineIds(int size) {
        return generateIds("CombinationLineSequence", size);
    }

    @Override
    public Deque<Integer> generateLineItemIds(int size) {
        return this.generateIds("LineItemSequence", size);
    }

    @Override
    public Deque<Integer> generateTaxLineIds(int size) {
        return this.generateIds("TaxLineSequence", size);
    }

    @Override
    public int generateShippingLineId() {
        return generateId("ShippingLineSequence");
    }

    @Override
    public int generateOrderDiscountCodeId() {
        return generateId("OrderDiscountCodeSequence");
    }

    @Override
    public Deque<Integer> generateDiscountApplicationIds(int size) {
        return generateIds("DiscountApplicationSequence", size);
    }

    @Override
    public Deque<Integer> generateDiscountAllocationIds(int size) {
        return generateIds("DiscountAllocationSequence", size);
    }

    @Override
    public int generateOrderId() {
        return generateId("OrderSequence");
    }

    @Override
    public Deque<Integer> generateRefundLineIds(int size) {
        return null;
    }

    @Override
    public int generateAdjustmentId() {
        return 0;
    }

    @Override
    public int generateRefundId() {
        return 0;
    }
}
