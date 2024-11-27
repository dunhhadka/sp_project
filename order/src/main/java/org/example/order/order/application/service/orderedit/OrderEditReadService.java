package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.CalculatedOrder;
import org.example.order.order.application.model.orderedit.OrderEditResponse;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderEditReadService {

    private OrderEditCalculatorService calculatorService;

    public OrderEditResponse getBeginEdit(OrderEditId orderEditId) {
        var calculatedOrder = calculateOrderEdit(orderEditId);
        return OrderEditResponse.builder().calculatedOrder(calculatedOrder).build();
    }

    private CalculatedOrder calculateOrderEdit(OrderEditId orderEditId) {
        return calculatorService.calculate(orderEditId);
    }


}
