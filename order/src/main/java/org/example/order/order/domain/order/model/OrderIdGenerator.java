package org.example.order.order.domain.order.model;

import java.util.Deque;

public interface OrderIdGenerator {

    int generateBillingAddressId();

    int generateShippingAddressId();

    Deque<Integer> generateCombinationLineIds(int size);

    Deque<Integer> generateLineItemIds(int size);

    Deque<Integer> generateTaxLineIds(int size);

    int generateShippingLineId();

    int generateOrderDiscountCodeId();

    Deque<Integer> generateDiscountApplicationIds(int size);

    Deque<Integer> generateDiscountAllocationIds(int size);

    int generateOrderId();

    Deque<Integer> generateRefundLineIds(int size);
}
