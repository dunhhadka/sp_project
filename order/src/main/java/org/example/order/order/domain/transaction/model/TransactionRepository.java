package org.example.order.order.domain.transaction.model;

import org.example.order.order.domain.order.model.OrderId;

import java.util.List;

public interface TransactionRepository {
    OrderTransaction getById(TransactionId transactionId);

    List<OrderTransaction> findByOrderId(OrderId id);
}
