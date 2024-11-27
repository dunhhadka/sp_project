package org.example.order.infrastructure.persistence;

import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderIdGenerator;
import org.example.order.order.domain.order.model.OrderIdGenerator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryIdGenerator implements OrderIdGenerator, FulfillmentOrderIdGenerator {

    private final AtomicInteger orderId = new AtomicInteger(0);
    private final AtomicInteger lineItemId = new AtomicInteger(0);
    private final AtomicInteger shippingLineId = new AtomicInteger(0);
    private final AtomicInteger discountApplicationId = new AtomicInteger(0);
    private final AtomicInteger discountAllocationId = new AtomicInteger(0);
    private final AtomicInteger fulfillmentOrderId = new AtomicInteger(0);
    private final AtomicInteger fulfillmentOrderLineItemId = new AtomicInteger(0);

    @Override
    public int generateBillingAddressId() {
        return 0;
    }

    @Override
    public int generateShippingAddressId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateCombinationLineIds(int size) {
        return null;
    }

    @Override
    public Deque<Integer> generateLineItemIds(int size) {
        return this.generateIds(size, lineItemId);
    }

    private Deque<Integer> generateIds(int size, AtomicInteger idHolder) {
        var maxValue = idHolder.addAndGet(size);
        Deque<Integer> result = new ArrayDeque<>(size);
        for (int i = maxValue - size + 1; i <= maxValue; i++) {
            result.addLast(i);
        }
        return result;
    }

    @Override
    public Deque<Integer> generateTaxLineIds(int size) {
        return null;
    }

    @Override
    public int generateShippingLineId() {
        return 0;
    }

    @Override
    public int generateOrderDiscountCodeId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateDiscountApplicationIds(int size) {
        return null;
    }

    @Override
    public Deque<Integer> generateDiscountAllocationIds(int size) {
        return null;
    }

    @Override
    public int generateOrderId() {
        return orderId.incrementAndGet();
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

    @Override
    public int generateFulfillmentOrderId() {
        return 0;
    }

    @Override
    public int generateFulfillmentOrderLineItemId() {
        return fulfillmentOrderLineItemId.incrementAndGet();
    }
}
