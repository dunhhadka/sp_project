package org.example.order.order.application.service.orderedit;

import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.orderedit.CalculatedDiscountAllocation;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.OrderEditDiscountAllocationDto;
import org.example.order.order.infrastructure.data.dto.OrderEditLineItemDto;
import org.example.order.order.infrastructure.data.dto.OrderEditTaxLineDto;

import java.util.List;

public class AddedLineItemBuilder extends AbstractLineItemBuilder<AddedLineItemBuilder.Context> {

    public AddedLineItemBuilder(OrderEditLineItemDto lineItem, Context context) {
        super(lineItem, context);
    }

    public static AddedLineItemBuilder forLineItem(OrderEditLineItemDto line, Context addedContext) {
        return new AddedLineItemBuilder(line, addedContext);
    }

    @Override
    protected void doBuild() {
        addChange(context().changes().action);
        var discounts = context().changes().addDiscounts;
        if (CollectionUtils.isNotEmpty(discounts)) {
            var discount = discounts.get(0); // support for a discount
            addChange(discount);

            lineItem()
                    .setCalculatedDiscountAllocations(
                            context().discountAllocations.stream()
                                    .map(CalculatedDiscountAllocation::new)
                                    .toList()
                    );
        }
    }

    record Changes(OrderStagedChange.AddLineItemAction action,
                   List<OrderStagedChange.AddItemDiscount> addDiscounts) {
    }

    public record Context(
            List<OrderEditTaxLineDto> taxLines,
            List<OrderEditDiscountAllocationDto> discountAllocations,
            Changes changes
    ) {
    }
}
