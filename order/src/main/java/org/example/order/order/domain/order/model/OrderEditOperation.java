package org.example.order.order.domain.order.model;

import java.util.List;

public interface OrderEditOperation {

    Order order();

    default void addNewLineItems(List<LineItem> lineItems) {
        lineItems.forEach(line -> line.setAggRoot(order()));

        order().getLineItems().addAll(lineItems);

        increaseTotalWeight(lineItems.stream().map(LineItem::getTotalWeight).reduce(0, Integer::sum));
    }

    default void increaseTotalWeight(Integer totalWeight) {
        int oldTotalWeight = order().getTotalWeight();
    }
}
