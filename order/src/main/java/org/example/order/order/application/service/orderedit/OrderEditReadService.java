package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.orderedit.CalculatedOrder;
import org.example.order.order.application.model.orderedit.OrderEditResponse;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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


    public OrderEditResponse getAddVariants(OrderEditId orderEditId, List<UUID> lineItemIds) {
        var calculatedOrder = calculatorService.calculate(orderEditId);

        var editBuilder = OrderEditResponse.builder()
                .calculatedOrder(calculatedOrder);

        if (CollectionUtils.isNotEmpty(lineItemIds)) {
            List<String> lineItemStrings = lineItemIds.stream().map(UUID::toString).toList();
            var editLineItems = calculatedOrder.getAddedLineItems().stream()
                    .filter(line -> lineItemStrings.contains(line.getId()))
                    .toList();
            var changes = editLineItems.stream()
                    .flatMap(line -> line.getStagedChanges().stream())
                    .toList();
            editBuilder
                    .calculatedLineItems(editLineItems)
                    .stagedChanges(changes);
        }

        return editBuilder.build();
    }
}
