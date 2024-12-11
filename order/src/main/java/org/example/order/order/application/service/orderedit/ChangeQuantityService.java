package org.example.order.order.application.service.orderedit;

import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ChangeQuantityService {


    public List<Pair<LineItem, Integer>> increaseQuantity(Order order, List<OrderStagedChange.IncrementItem> incrementItems) {
        if (CollectionUtils.isEmpty(incrementItems)) return List.of();
        return order.increaseQuantity(
                incrementItems.stream()
                        .collect(Collectors.toMap(
                                OrderStagedChange.IncrementItem::getLineItemId,
                                OrderStagedChange.IncrementItem::getDelta))
        );
    }

}
