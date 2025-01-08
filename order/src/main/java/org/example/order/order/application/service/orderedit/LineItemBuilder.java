package org.example.order.order.application.service.orderedit;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.orderedit.CalculatedDiscountAllocation;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class LineItemBuilder extends AbstractLineItemBuilder<LineItemBuilder.Context> {

    public LineItemBuilder(LineItemDto lineItem, Context context) {
        super(lineItem, context);
    }

    @Override
    protected Stream<? extends GenericTaxLine> streamTaxLines() {
        return calculateTaxes();
    }

    private Stream<? extends GenericTaxLine> calculateTaxes() {
        if (!context().lineItem.isTaxable()) {
            return Stream.empty();
        }

        var action = context().action;
        if (action == null) {
            return exitingTaxLines();
        }
        if (action instanceof OrderStagedChange.IncrementItem) {
            return Stream.concat(exitingTaxLines(), context().addedTaxLines.stream());
        }

        OrderStagedChange.DecrementItem decrement = (OrderStagedChange.DecrementItem) context().action;
        int[] amount = {decrement.getDelta()};

        return context().taxLines
                .stream()
                .sorted(Comparator.<CombinedTaxLine>
                                comparingInt(combined -> combined.taxLine.getId())
                        .reversed()
                )
                .map(combined -> new MergedTaxLine(MergedTaxLine.TaxLineKey.from(combined.taxLine))
                        .merge(combined.taxLine)
                        .mergeAll(combined.refundedTaxLines)
                )
                .dropWhile(tax -> {
                    amount[0] -= tryReduce(tax, amount[0]);
                    return tax.getQuantity() <= 0;
                });
    }

    private int tryReduce(MergedTaxLine tax, int amount) {
        int reducible = tax.getQuantity();
        if (amount <= reducible) {
            tax.reduce(amount);
            return amount;
        }
        return 0;
    }

    private Stream<? extends GenericTaxLine> exitingTaxLines() {
        return context().taxLines.stream()
                .map(combined ->
                        new MergedTaxLine(MergedTaxLine.TaxLineKey.from(combined.taxLine))
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
            addChange(action);
            adjustQuantity(action);
        } else {
            disallowQuantityAdjustment();
        }

        applyDiscount();
    }

    private void applyDiscount() {
        lineItem().setCalculatedDiscountAllocations(
                context().discountAllocations.stream().map(CalculatedDiscountAllocation::new).toList());

        lineItem().setDiscountedUnitPrice(context().lineItem.getDiscountUnitPrice());

        BigDecimal quantity = BigDecimal.valueOf(lineItem().getQuantity());
        BigDecimal editableQuantity = BigDecimal.valueOf(lineItem().getEditableQuantity());

        BigDecimal totalPriceAfterDiscounted = lineItem().getOriginalUnitPrice().multiply(quantity);

        if (context().hasAnyDiscount()) {
            assert context().action == null : "Line item has discount, no quantity adjustment should be present";

            BigDecimal totalDiscount = context().discountAllocations.stream()
                    .filter(context().isOrderDiscount.negate())
                    .map(DiscountAllocationDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalPriceAfterDiscounted = totalPriceAfterDiscounted.subtract(totalDiscount);
        }

        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            lineItem().setEditableSubtotal(
                    totalPriceAfterDiscounted
                            .multiply(editableQuantity)
                            .divide(quantity, RoundingMode.HALF_UP));
        } else {
            lineItem().setEditableSubtotal(BigDecimal.ZERO);
        }

        lineItem().setUneditableSubtotal(totalPriceAfterDiscounted.subtract(lineItem().getEditableSubtotal()));
    }

    private void adjustQuantity(OrderStagedChange.QuantityAdjustmentAction action) {
        if (context().hasAnyDiscount()) {
            Preconditions.checkArgument(action == null || action.getDelta() == context().lineItem.getQuantity(),
                    "Cannot adjust quantity of line items with discounts");
            disallowQuantityAdjustment();
            return;
        }

        int editableQuantity = context().lineItem.getFulfillableQuantity();
        int quantity = context().lineItem.getQuantity();
        int delta = action.getDelta();

        int newQuantity;
        int newEditableQuantity;

        if (action instanceof OrderStagedChange.IncrementItem) {
            newQuantity = quantity + delta;
            newEditableQuantity = editableQuantity + delta;
        } else if (action instanceof OrderStagedChange.DecrementItem di) {
            Verify.verify(editableQuantity >= delta);
            newQuantity = quantity - delta;
            newEditableQuantity = editableQuantity - delta;
            lineItem().setRestocking(di.isRestock());
        } else throw new IllegalStateException("Unknown implementation of QuantityAdjustmentAction");

        lineItem().setQuantity(newQuantity);
        lineItem().setEditableQuantity(newEditableQuantity);
        lineItem().setEditableQuantityBeforeChange(editableQuantity);
    }

    private void disallowQuantityAdjustment() {
        int quantity = context().lineItem.getCurrentQuantity();
        int editableQuantity = context().lineItem.getFulfillableQuantity();

        lineItem().setQuantity(quantity);
        lineItem().setEditableQuantity(editableQuantity);
        lineItem().setEditableQuantityBeforeChange(editableQuantity);
    }

}
