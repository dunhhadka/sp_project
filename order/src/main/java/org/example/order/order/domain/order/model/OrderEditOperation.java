package org.example.order.order.domain.order.model;

import org.example.order.order.application.service.orderedit.AddService;

import java.util.List;
import java.util.Map;

public interface OrderEditOperation {

    Order order();

    default List<LineItem> editAddNewLineItems(List<LineItem> lineItems, Map<Integer, AddService.DiscountLineItem> discountMap) {
        addNewLineItems(lineItems);


        return lineItems;
    }

    default void addNewLineItems(List<LineItem> lineItems) {
        lineItems.forEach(line -> line.setAggRoot(order()));

        order().getLineItems().addAll(lineItems);

        increaseTotalWeight(lineItems.stream().map(LineItem::getTotalWeight).reduce(0, Integer::sum));
    }

    default void increaseTotalWeight(Integer totalWeight) {
        int oldTotalWeight = order().getTotalWeight();
    }
}
