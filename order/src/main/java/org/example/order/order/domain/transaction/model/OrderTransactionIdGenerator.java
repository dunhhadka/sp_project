package org.example.order.order.domain.transaction.model;

import java.util.Deque;

public interface OrderTransactionIdGenerator {

    Deque<Integer> generateOrderTransactionIds(int size);
}
