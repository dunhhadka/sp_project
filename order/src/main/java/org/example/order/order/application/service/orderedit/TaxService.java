package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class TaxService {

    public void addTaxForLineItems(
            Order order,
            List<LineItem> newLineItems,
            List<OrderCommitService.ChangedLineItem> changedLineItems
    ) {
        if (order.isTaxExempt() ||
                (CollectionUtils.isEmpty(newLineItems) && CollectionUtils.isEmpty(changedLineItems)))
            return;

        var allLineChanged = Stream.concat(
                        newLineItems.stream(),
                        changedLineItems.stream()
                                .map(OrderCommitService.ChangedLineItem::lineItem))
                .toList();
        boolean notShouldAddTax = allLineChanged.stream().noneMatch(LineItem::isTaxable);
        if (!notShouldAddTax)
            return;

        int lineItemCount = allLineChanged.size();
        Set<Integer> productIds = allLineChanged.stream()
                .map(change -> change.getVariantInfo().getProductId())
                .filter(NumberUtils::isPositive)
                .collect(Collectors.toSet());
        productIds.add(0); // set default for calculate tax

        
    }
}
