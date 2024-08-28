package org.example.order.order.application.service.order;

import lombok.Builder;
import lombok.Getter;
import org.example.order.order.domain.order.model.Order;

import java.util.List;

@Getter
@Builder
public class OrderPaymentResult {
    private boolean isFromCheckout;
    private String checkoutToken;
    private List<Integer> paymentIds;
    private List<Order.TransactionInput> transactions;
}
