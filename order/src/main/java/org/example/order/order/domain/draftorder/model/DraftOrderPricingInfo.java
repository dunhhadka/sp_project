package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DraftOrderPricingInfo {
    /**
     * Tổng phụ của đơn hàng = Tổng giá trị đơn hàng - giảm giá sản phẩm - giảm giá đơn hàng
     * = line_items_subtotal_price - applied_discount.amount
     */
    @NotNull
    private BigDecimal subtotalPrice;

    /**
     * Tống số tiền của đơn hàng nháp (bao gồm thuế, phí vận chuyển và giảm giá) <br/>
     * Nếu taxable = true: total_price = subtotal_price + total_shipping_price <br/>
     * Nếu taxable = false: total_price = subtotal_price + total_shipping_price + total_tax
     */
    @NotNull
    private BigDecimal totalPrice;

    /**
     * Tổng khuyến mãi bao gồm khuyến mãi ở cấp độ sản phẩm và cấp độ đơn hàng:
     */
    private BigDecimal totalDiscounts;

    /**
     * Tổng phí vận chuyển đơn hàng = shipping_line.price
     */
    private BigDecimal totalShippingPrice;

    /**
     * Tổng giá trị lineItem không bao gồm bất kỳ giảm giá nào = Tổng LineItem.totalOriginal
     */
    private BigDecimal totalLineItemPrice;

    /**
     * Tổng giá trị tất cả lineItem đã giảm giá sản phẩm nhưng chưa bao gồm khuyến mãi ở cấp đợ đơn hàng,
     * chưa bao gồm phí vận chuyển, có thể bao gồm thuế, hoặc chưa bao gồm thuế, tùy vào cấu hình giá sản phẩm
     * đã bao gồm thuế/chưa bao gòm thuế.
     * = Tổng LineItem.discountedTotal, trong đó discountedTotal là giá trị của một lineItem đã trừ đi giảm giá sản phẩm
     */
    private BigDecimal lineItemSubtotalPrice;

    /**
     * Tổng giá trị thuế = Tổng TaxLine.price
     */
    private BigDecimal totalTax;

}
