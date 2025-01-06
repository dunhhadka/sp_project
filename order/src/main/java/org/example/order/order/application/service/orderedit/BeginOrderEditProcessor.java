package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class BeginOrderEditProcessor extends AbstractOrderEditProcessor {

    private final OrderRepository orderRepository;

    @Override
    protected ShippingData calculateShipping(Order order) {
        BigDecimal totalShipping = BigDecimal.ZERO;
        BigDecimal shippingDiscountAmount = BigDecimal.ZERO;
        BigDecimal totalShippingTax = BigDecimal.ZERO;

        if (CollectionUtils.isEmpty(order.getShippingLines()))
            return ShippingData.empty();
        for (var shippingLine : order.getShippingLines()) {
            totalShipping = totalShipping.add(shippingLine.getPrice());

            if (CollectionUtils.isNotEmpty(shippingLine.getDiscountAllocations())) {
                shippingDiscountAmount = shippingDiscountAmount.add(
                        shippingLine.getDiscountAllocations().stream()
                                .map(DiscountAllocation::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
            }

            if (CollectionUtils.isNotEmpty(shippingLine.getTaxLines())) {
                totalShippingTax = totalShippingTax.add(
                        shippingLine.getTaxLines().stream()
                                .map(TaxLine::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        }
        return new ShippingData(
                totalShipping,
                shippingDiscountAmount,
                totalShippingTax
        );
    }

    @Override
    protected RefundedData calculateRefunds(Order order) {
        Map<Integer, BigDecimal> refundedMap = new HashMap<>();
        BigDecimal totalShippingRefunded = BigDecimal.ZERO;
        BigDecimal refundedTaxAmount = BigDecimal.ZERO;

        if (CollectionUtils.isEmpty(order.getRefunds())) {
            return RefundedData.empty();
        }

        for (var refund : order.getRefunds()) {
            for (var refundLine : refund.getRefundLineItems()) {
                refundedMap.merge(refundLine.getLineItemId(), refundLine.getSubtotal(), BigDecimal::add);
            }
            for (var adjustment : refund.getOrderAdjustments()) {
                if (adjustment.getRefundKind() == OrderAdjustment.RefundKind.shipping_refund) {
                    totalShippingRefunded = totalShippingRefunded.add(adjustment.getAmount());
                    if (NumberUtils.isPositive(adjustment.getTaxAmount())) {
                        refundedTaxAmount = refundedTaxAmount.add(adjustment.getTaxAmount());
                    }
                }
            }
        }

        return new RefundedData(
                refundedMap,
                totalShippingRefunded,
                refundedTaxAmount);
    }

    @Override
    protected Order findOrder(int storeId, int orderId) {
        return orderRepository.findById(new OrderId(storeId, orderId));
    }

    @Override
    protected OrderEditValidator getValidator() {
        return new OrderEditValidatorImpl();
    }

    @Override
    protected Predicate<DiscountAllocation> isOrderDiscount(List<DiscountApplication> discountApplications) {
        return allocation -> {
            var application = discountApplications.get(allocation.getApplicationIndex());
            return application.getRuleType() == DiscountApplication.RuleType.order;
        };
    }
}
