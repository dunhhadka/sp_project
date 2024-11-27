package org.example.order.order.application.service.orderedit;

import jakarta.annotation.Nullable;
import org.example.order.order.application.model.orderedit.CalculatedDiscountAllocation;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.OrderEditDiscountAllocationDto;
import org.example.order.order.infrastructure.data.dto.OrderEditLineItemDto;
import org.example.order.order.infrastructure.data.dto.OrderEditTaxLineDto;

import javax.xml.transform.sax.SAXResult;
import java.util.List;
import java.util.stream.Stream;

public final class AddedLineItemBuilder extends AbstractLineItemBuilder<AddedLineItemBuilder.Context> {

    public AddedLineItemBuilder(OrderEditLineItemDto addedLineItem, Context context) {
        super(addedLineItem, context);
    }

    /**
     * còn thiếu addTaxLine, tính toán lại khi apply discount
     */
    @Override
    protected void doBuild() {
        addChange(context.changes.addAction);
        if (context.changes.addItemDiscount != null) {
            addChange(context.changes.addItemDiscount);
            lineItem()
                    .setCalculatedDiscountAllocations(
                            context.allocation.stream()
                                    .map(CalculatedDiscountAllocation::new)
                                    .toList())
                    .setHasStagedLineItemDiscount(true);
        }
    }

    @Override
    Stream<? extends GenerateTaxLine> streamTaxLines() {
        return Stream.ofNullable(context.taxLine);
    }

    /**
     * Tại AddedLineItemBuilder chỉ thực hiện các thao tác bổ sung thêm discount của line
     * hầu hết các bước build LineItem sẽ ở abstractBuilder
     */
    public static AddedLineItemBuilder forLineItem(OrderEditLineItemDto addedLineItem, Context context) {
        return new AddedLineItemBuilder(addedLineItem, context);
    }

    record Changes(OrderStagedChange.AddLineItemAction addAction,
                   @Nullable OrderStagedChange.AddItemDiscount addItemDiscount) {
    }

    public record Context(OrderEditTaxLineDto taxLine,
                          List<OrderEditDiscountAllocationDto> allocation,
                          Changes changes) {
    }
}
