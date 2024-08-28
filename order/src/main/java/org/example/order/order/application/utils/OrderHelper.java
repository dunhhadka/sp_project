package org.example.order.order.application.utils;

import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.MoneyInfo;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

public final class OrderHelper {

    public static MoneyInfo recalculateMoneyInfo(Order order, List<OrderTransaction> transactions) {
        var currentCartLevelDiscount = getCurrentCartLevelDiscount(order);
        var currentDiscountedTotal = getCurrentDiscountedTotal(order);
        return MoneyInfo.builder().build();
    }

    private static BigDecimal getCurrentDiscountedTotal(Order order) {
        var discountedTotal = order.getLineItems().stream()
                .map(LineItem::getDiscountedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (CollectionUtils.isEmpty(order.getRefunds())) return discountedTotal;
        return BigDecimal.ZERO;
    }

    private static BigDecimal getCurrentCartLevelDiscount(Order order) {
        var cartLevelDiscount = order.getLineItems().stream()
                .map(LineItem::getOrderDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (CollectionUtils.isEmpty(order.getRefunds())) {
            return cartLevelDiscount;
        }

        var refundedCartLevelDiscount = order.getRefunds().stream()
                .map(Refund::getTotalCartDiscountRefunded)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cartLevelDiscount.subtract(refundedCartLevelDiscount);
    }
}
