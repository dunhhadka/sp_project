package org.example.order.order.application.service.orderedit;

import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.orderedit.CalculatedDiscountAllocation;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;

public final class LineItemBuilder extends AbstractLineItemBuilder<LineItemBuilder.Context> {

    private LineItemBuilder(LineItemDto lineItem, Context context) {
        super(lineItem, context);
    }

    public static LineItemBuilder forLineItem(LineItemDto orderLine, Context context) {
        return new LineItemBuilder(orderLine, context);
    }

    @Override
    protected void doBuild() {
        initQuantity();

        applyQuantityAdjustment(context.action);

        applyDiscount();
    }

    @Override
    Stream<? extends GenerateTaxLine> streamTaxLines() {
        return calculateTax();
    }

    private Stream<? extends GenerateTaxLine> calculateTax() {
        if (!context.orderLine.isTaxable()) {
            return Stream.empty();
        }

        var action = context.action;
        if (action == null) {
            return existingTaxLines();
        }

        if (action instanceof OrderStagedChange.IncrementItem) {
            return Stream.concat(existingTaxLines(), Stream.of(context.newTaxes));
        } else if (action instanceof OrderStagedChange.DecrementItem decrementItem) {
            return reduceTaxLine(decrementItem);
        }
        throw new IllegalArgumentException("error");
    }

    private Stream<? extends GenerateTaxLine> reduceTaxLine(OrderStagedChange.DecrementItem decrementItem) {
        return Stream.of();
    }

    private Stream<? extends GenerateTaxLine> existingTaxLines() {
        return context.existingTaxLines.stream()
                .map(combined ->
                        new MergedTaxLine(combined.taxLine)
                                .merge(combined.taxLine)
                                .mergeRefunds(combined.refundTaxLines));
    }

    private void applyDiscount() {
        var allocations = context.allocations;
        if (CollectionUtils.isEmpty(allocations)) return;

        if (context.action != null) {
            throw new IllegalArgumentException("line item action not supported for discount_line");
        }

        lineItem()
                .setCalculatedDiscountAllocations(
                        allocations.stream()
                                .map(CalculatedDiscountAllocation::new)
                                .toList());

        var quantity = BigDecimal.valueOf(lineItem().getQuantity());
        var originalPrice = context.orderLine.getPrice();

        var totalDiscountAmount = allocations.stream()
                .filter(context.isProductDiscount)
                .map(DiscountAllocationDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var originalTotalPrice = quantity.multiply(originalPrice);
        var discountedTotalPrice = originalTotalPrice.subtract(totalDiscountAmount);

        lineItem().setEditableSubtotal(
                discountedTotalPrice
                        .multiply(quantity)
                        .divide(BigDecimal.valueOf(lineItem().getEditableQuantity()), RoundingMode.CEILING));
        lineItem().setUneditableSubtotal(
                discountedTotalPrice
                        .subtract(lineItem().getEditableSubtotal()));
    }

    private void initQuantity() {
        int quantity = context.orderLine.getQuantity();
        int editableQuantity = context.orderLine.getFulfillableQuantity();

        lineItem()
                .setQuantity(quantity)
                .setEditableQuantity(editableQuantity)
                .setEditableQuantityBeforeChange(editableQuantity);
    }

    private void applyQuantityAdjustment(OrderStagedChange.QuantityAdjustmentAction action) {
        if (CollectionUtils.isNotEmpty(context.allocations))
            return; // không được điều chỉnh số lượng của line đã có discount
        if (action == null) return;

        int quantity = lineItem().getQuantity();
        int editableQuantity = lineItem().getEditableQuantity();
        int delta = action.getDelta();

        int newQuantity;
        int newEditableQuantity;
        if (action instanceof OrderStagedChange.IncrementItem ii) {
            newQuantity = quantity + delta;
            newEditableQuantity = editableQuantity + delta;
            addChange(ii);
        } else if (action instanceof OrderStagedChange.DecrementItem di) {
            if (delta > editableQuantity) throw new IllegalArgumentException("error decrease line item");
            newQuantity = quantity - delta;
            newEditableQuantity = editableQuantity - delta;
            addChange(di);
        } else {
            throw new IllegalArgumentException("not supported for action line item");
        }

        lineItem()
                .setQuantity(newQuantity)
                .setEditableQuantity(newEditableQuantity)
                .setEditableQuantityBeforeChange(editableQuantity);
    }

    public record TaxLineInfo(TaxLineDto taxLine, List<RefundTaxLineDto> refundTaxLines) {

    }

    public record Context(
            OrderStagedChange.QuantityAdjustmentAction action,
            List<DiscountAllocationDto> allocations,
            List<TaxLineInfo> existingTaxLines,
            OrderEditTaxLineDto newTaxes, LineItemDto orderLine,
            java.util.function.Predicate<DiscountAllocationDto> isProductDiscount) {

    }
}
