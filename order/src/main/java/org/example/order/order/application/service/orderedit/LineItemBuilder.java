package org.example.order.order.application.service.orderedit;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.orderedit.CalculatedDiscountAllocation;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class LineItemBuilder extends AbstractLineItemBuilder<LineItemBuilder.Context> {

    public LineItemBuilder(LineItemDto lineItem, Context context) {
        super(lineItem, context);
    }

    @Override
    protected Stream<? extends GenericTaxLine> streamTaxLines() {
        return collectTaxLines();
    }

    private Stream<? extends GenericTaxLine> collectTaxLines() {
        if (!context().lineItem.isTaxable()) {
            return Stream.empty();
        }

        var action = context().action;
        if (action == null) {
            return existingTaxLines();
        }
        if (action instanceof OrderStagedChange.IncrementItem) {
            return Stream.concat(existingTaxLines(), context().addedTaxLines.stream());
        }

        OrderStagedChange.DecrementItem decrement = (OrderStagedChange.DecrementItem) action;
        int delta = decrement.getDelta();
        return null;
    }

    private Stream<? extends GenericTaxLine> existingTaxLines() {
        return context().taxLines.stream()
                .map(combined -> new MergedTaxLine(MergedTaxLine.TaxLineKey.from(combined.taxLine()))
                        .merge(combined.taxLine)
                        .mergeAll(combined.refundedTaxLines));
    }

    public static LineItemBuilder forLineItem(LineItemDto lineItem, Context context) {
        return new LineItemBuilder(lineItem, context);
    }

    public record CombinedTaxLine(TaxLineDto taxLine, List<RefundTaxLineDto> refundedTaxLines) {
    }

    public record Context(
            List<CombinedTaxLine> taxLines,
            List<DiscountAllocationDto> discountAllocations,
            OrderStagedChange.QuantityAdjustmentAction action,
            List<OrderEditTaxLineDto> addedTaxLines,
            LineItemDto lineItem,
            Predicate<DiscountAllocationDto> isOrderDiscount) {
        public boolean hasAnyDiscount() {
            return CollectionUtils.isNotEmpty(discountAllocations);
        }
    }

    @Override
    protected void doBuild() {
        var action = context().action;
        if (action != null) {
            // add action to staged changes
            addChange(action);
            // apply change quantity
            adjustQuantity(action);
        } else {
            disallowAdjustQuantity();
        }

        applyDiscount();
    }

    private void applyDiscount() {
        BigDecimal quantity = BigDecimal.valueOf(lineItem().getQuantity());
        BigDecimal editableQuantity = BigDecimal.valueOf(lineItem().getEditableQuantity());

        BigDecimal totalPriceAfterDiscounted = lineItem().getOriginalUnitPrice().multiply(quantity);
        if (context().hasAnyDiscount()) {
            assert context().action == null : "Line item has discount, no quantity adjustment should be present";

            lineItem().setCalculatedDiscountAllocations(
                    context().discountAllocations.stream()
                            .map(CalculatedDiscountAllocation::new)
                            .toList());

            BigDecimal totalDiscountPrice = context().discountAllocations.stream()
                    .map(DiscountAllocationDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalPriceAfterDiscounted = totalPriceAfterDiscounted.subtract(totalDiscountPrice);
        }

        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            lineItem().setEditableSubtotal(
                    totalPriceAfterDiscounted
                            .multiply(editableQuantity)
                            .divide(quantity, RoundingMode.HALF_UP));
        } else {
            lineItem().setEditableSubtotal(BigDecimal.ZERO);
        }

        lineItem().setDiscountedUnitPrice(context().lineItem.getDiscountUnitPrice());

        lineItem().setUneditableSubtotal(totalPriceAfterDiscounted.subtract(lineItem().getEditableSubtotal()));
    }

    private void adjustQuantity(OrderStagedChange.QuantityAdjustmentAction action) {
        if (context().hasAnyDiscount()) {
            Preconditions.checkArgument(action == null || action.getDelta() == context().lineItem.getFulfillableQuantity(),
                    "Cannot adjust quantity of line item with discounts");
            disallowAdjustQuantity();
            return;
        }

        int quantity = context().lineItem.getQuantity();
        int editableQuantity = context().lineItem.getFulfillableQuantity();
        int delta = action.getDelta();

        int newQuantity;
        int newEditableQuantity;

        if (action instanceof OrderStagedChange.IncrementItem) {
            newQuantity = quantity + delta;
            newEditableQuantity = editableQuantity + delta;
        } else if (action instanceof OrderStagedChange.DecrementItem decrement) {
            Verify.verify(delta <= editableQuantity);

            newQuantity = quantity - delta;
            newEditableQuantity = editableQuantity - delta;

            lineItem().setRestocking(decrement.isRestock());
        } else throw new IllegalArgumentException("Unknown implementation for QuantityAdjustmentAction");

        lineItem().setQuantity(newQuantity);
        lineItem().setEditableQuantity(newEditableQuantity);
        lineItem().setEditableQuantityBeforeChange(editableQuantity);
    }

    private void disallowAdjustQuantity() {
        int quantity = context().lineItem.getQuantity();
        int editableQuantity = context().lineItem.getFulfillableQuantity();

        lineItem().setQuantity(quantity);
        lineItem().setEditableQuantity(editableQuantity);
        lineItem().setEditableQuantityBeforeChange(editableQuantity);
    }
}
