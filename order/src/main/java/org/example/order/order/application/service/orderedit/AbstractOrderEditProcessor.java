package org.example.order.order.application.service.orderedit;

import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.DiscountAllocation;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.TaxLine;
import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.domain.orderedit.model.OrderEditId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AbstractOrderEditProcessor {

    public final OrderEditId beginEdit(int storeId, int orderId) {
        var order = findOrder(storeId, orderId);

        getValidator().validateOrder(order);

        RefundedData refundedMap = calculateRefunds(order);

        ShippingData shippingData = calculateShipping(order);

        CalculatedData calculatedData = calculateOrderEditable(order, refundedMap, shippingData);

        var totalPrice = calculatedData.calculateTotalPrice();

        var orderEdit = new OrderEdit(
                order.getMoneyInfo().getCurrency(),
                orderId,
                calculatedData.subtotalLineItemQuantity,
                calculatedData.subtotalPrice,
                calculatedData.cartDiscountAmount,
                totalPrice,
                order.getMoneyInfo().getTotalOutstanding()
        );

        return orderEdit.getId();
    }


    private CalculatedData calculateOrderEditable(Order order, RefundedData refundedData, ShippingData shippingData) {
        BigDecimal subtotalLineItemQuantity = BigDecimal.ZERO;
        BigDecimal subtotalPrice = BigDecimal.ZERO;
        BigDecimal totalTaxLine = BigDecimal.ZERO;
        BigDecimal cartDiscountAmount = BigDecimal.ZERO;

        Currency currency = order.getMoneyInfo().getCurrency();

        Map<Integer, BigDecimal> refundedMap = refundedData.refundedLine;

        for (var lineItem : order.getLineItems()) {
            int lineItemId = lineItem.getId();

            BigDecimal originalQuantityDecimal = BigDecimal.valueOf(lineItem.getQuantity());
            BigDecimal refundedQuantity = refundedMap.getOrDefault(lineItemId, BigDecimal.ZERO);
            BigDecimal currentQuantity = originalQuantityDecimal.subtract(refundedQuantity);
            if (!NumberUtils.isPositive(currentQuantity))
                continue;

            subtotalLineItemQuantity = subtotalLineItemQuantity.add(currentQuantity);
            subtotalPrice = subtotalPrice.add(currentQuantity.multiply(lineItem.getPrice()));

            if (CollectionUtils.isNotEmpty(lineItem.getDiscountAllocations())) {
                BigDecimal productDiscount = BigDecimal.ZERO;
                BigDecimal orderDiscount = BigDecimal.ZERO;
                for (var discount : lineItem.getDiscountAllocations()) {
                    if (isOrderDiscount(order.getDiscountApplications()).test(discount)) {
                        orderDiscount = orderDiscount.add(discount.getAmount());
                    } else {
                        productDiscount = productDiscount.add(discount.getAmount());
                    }
                }

                BigDecimal effectiveProductDiscount = calculateEffectivePrice(productDiscount, currentQuantity, originalQuantityDecimal, currency);
                subtotalPrice = subtotalPrice.subtract(effectiveProductDiscount);

                BigDecimal effectiveOrderDiscount = calculateEffectivePrice(orderDiscount, currentQuantity, originalQuantityDecimal, currency);
                cartDiscountAmount = cartDiscountAmount.add(effectiveOrderDiscount);
            }

            if (CollectionUtils.isNotEmpty(lineItem.getTaxLines())) {
                var totalTaxLinePrice = lineItem.getTaxLines().stream()
                        .map(TaxLine::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalTaxLine = totalTaxLine.add(calculateEffectivePrice(totalTaxLinePrice, currentQuantity, originalQuantityDecimal, currency));

            }
        }

        return new CalculatedData(
                subtotalLineItemQuantity,
                subtotalPrice,
                shippingData.totalShipping,
                shippingData.totalShippingTax,
                totalTaxLine,
                cartDiscountAmount,
                shippingData.shippingDiscountAmount,
                shippingData.totalShippingTax
        );
    }

    private BigDecimal calculateEffectivePrice(BigDecimal price, BigDecimal currentQuantity,
                                               BigDecimal originalQuantity, Currency currency) {
        if (price == null || price.signum() == 0)
            return BigDecimal.ZERO;

        return price.multiply(currentQuantity)
                .divide(originalQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
    }

    protected abstract ShippingData calculateShipping(Order order);

    protected abstract RefundedData calculateRefunds(Order order);

    protected abstract Order findOrder(int storeId, int orderId);

    protected abstract OrderEditValidator getValidator();

    protected abstract Predicate<DiscountAllocation> isOrderDiscount(List<DiscountApplication> applications);

    private record CalculatedData(

            BigDecimal subtotalLineItemQuantity,
            BigDecimal subtotalPrice,
            BigDecimal totalShipping,
            BigDecimal totalShippingTax,
            BigDecimal totalTaxLine,
            BigDecimal cartDiscountAmount,
            BigDecimal shippingDiscountAmount,
            BigDecimal totalShippingRefunded
    ) {

        public BigDecimal calculateTotalPrice() {
            return subtotalPrice
                    .add(totalShipping)
                    .add(totalShippingTax)
                    .add(totalTaxLine)
                    .subtract(cartDiscountAmount)
                    .subtract(shippingDiscountAmount)
                    .subtract(totalShippingRefunded);
        }
    }

    protected record ShippingData(
            BigDecimal totalShipping,
            BigDecimal shippingDiscountAmount,
            BigDecimal totalShippingTax
    ) {
        public static ShippingData empty() {
            return new ShippingData(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    protected record RefundedData(
            Map<Integer, BigDecimal> refundedLine,
            BigDecimal totalRefundedShipping,
            BigDecimal refundedTaxAmount
    ) {
        public static RefundedData empty() {
            return new RefundedData(Map.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
