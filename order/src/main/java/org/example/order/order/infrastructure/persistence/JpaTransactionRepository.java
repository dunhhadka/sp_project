package org.example.order.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.model.TransactionId;
import org.example.order.order.domain.transaction.model.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaTransactionRepository implements TransactionRepository {
    @Override
    public OrderTransaction getById(TransactionId transactionId) {
        return null;
    }

    @Override
    public List<OrderTransaction> findByOrderId(OrderId id) {
        return List.of();
    }
}
