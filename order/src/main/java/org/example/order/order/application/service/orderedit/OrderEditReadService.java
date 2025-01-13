package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.OrderEditResponse;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    public OrderEditResponse getEditWithAddedLines(OrderEditId orderEditId, List<UUID> addedLineItemIds) {
        var calculatedOrder = calculatorService.calculateResponse(orderEditId);

        var addedLineItems = calculatedOrder.getAddedLineItems().stream()
                .filter(line -> addedLineItemIds.stream().allMatch(id -> Objects.equals(id.toString(), line.getId())))
                .toList();

        return OrderEditResponse.builder()
                .calculatedOrder(calculatedOrder)
                .calculatedLineItems(addedLineItems)
                .build();
    }

    public OrderEditResponse getCalculatedLineItem(OrderEditId orderEditId, String lineItemId) {
        var calculatedOrder = calculatorService.calculateResponse(orderEditId);

        var addedLineItem = calculatedOrder.getLineItems().stream()
                .filter(line -> Objects.equals(line.getId(), lineItemId))
                .findFirst().orElse(null);
        assert addedLineItem != null;

        return OrderEditResponse.builder()
                .calculatedOrder(calculatedOrder)
                .calculatedLineItems(List.of(addedLineItem))
                .build();
    }
}
