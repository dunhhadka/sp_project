package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Currency;

@Getter
@Embeddable
@AllArgsConstructor
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyInfo {
    /**
     * totalPrice (Tổng tiền thanh toán)
     * Ý nghĩa: Đây là tổng số tiền mà khách hàng phải thanh toán thực tế cho đơn hàng, bao gồm cả tổng tiền hàng (subTotalPrice) và tất cả các loại phí, thuế, giảm giá đã được tính toán.
     * Tính toán: Được tính bằng cách cộng subTotalPrice với các loại phí và trừ đi các loại giảm giá.
     */
    private @NotNull BigDecimal totalPrice;

    /**
     * subTotalPrice (Tổng tiền hàng)
     * Ý nghĩa: Đây là tổng giá trị của tất cả các sản phẩm trong đơn hàng, chưa bao gồm các loại phí phụ trội như phí vận chuyển, thuế, hoặc các khoản giảm giá.
     * Tính toán: Được tính bằng cách cộng giá của từng sản phẩm (số lượng * đơn giá) lại với nhau.
     */
    private @NotNull BigDecimal subtotalPrice;

    private @NotNull BigDecimal totalLineItemPrice;

    @Builder.Default
    private BigDecimal originalTotalPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal cartDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalShippingPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentTotalPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentSubtotalPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentTotalDiscount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentCartDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentTotalTax = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalOutstanding = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal unpaidAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalRefund = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalReceived = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal netPayment = BigDecimal.ZERO;

    private @NotNull Currency currency;
}
