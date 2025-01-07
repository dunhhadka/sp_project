package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.OrderEditResponse;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderEditReadService {

    private final OrderEditCalculatorService calculatorService;

    public OrderEditResponse getBeginEdit(OrderEditId editId) {
        var calculatedOrder = calculatorService.calculateResponse(editId);

        return OrderEditResponse.builder()
                .calculatedOrder(calculatedOrder)
                .build();
    }
}
