package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.domain.order.model.Order;

import java.util.List;

public class OrderEditValidatorImpl implements OrderEditValidator {
    @Override
    public void validateOrder(Order order) {
        if (order == null) {
            throw new NotFoundException();
        }
        if (order.getCancelledOn() != null) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("invalid")
                    .message("order cancelled can't be edit")
                    .fields(List.of("order_id"))
                    .build());
        }
        if (order.getClosedOn() != null) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("invalid")
                    .message("order cancelled can't be edit")
                    .fields(List.of("order_id"))
                    .build());
        }
    }
}
