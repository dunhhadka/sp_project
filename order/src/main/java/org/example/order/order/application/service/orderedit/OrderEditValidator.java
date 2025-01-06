package org.example.order.order.application.service.orderedit;

import org.example.order.order.domain.order.model.Order;

public interface OrderEditValidator {
    void validateOrder(Order order);
}
