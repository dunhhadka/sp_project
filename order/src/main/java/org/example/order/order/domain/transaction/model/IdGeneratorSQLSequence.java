package org.example.order.order.domain.transaction.model;

import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;

@Repository
public class IdGeneratorSQLSequence implements OrderTransactionIdGenerator {
    @Override
    public Deque<Integer> generateOrderTransactionIds(int size) {
        return new ArrayDeque<>();
    }
}
