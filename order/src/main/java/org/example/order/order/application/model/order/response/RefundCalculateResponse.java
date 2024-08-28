package org.example.order.order.application.model.order.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.example.order.order.domain.refund.model.RefundLineItem;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class RefundCalculateResponse {

    private Shipping shipping;
    private List<LineItem> refundItems;
    private List<LineItem> refundableItems;

    private BigDecimal maximumRefundable;

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Shipping {
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;
        private BigDecimal maximumRefundable = BigDecimal.ZERO;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LineItem {
        private int quantity;
        private int lineItemId;
        private Long locationId;

        private BigDecimal price = BigDecimal.ZERO;
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal totalTax = BigDecimal.ZERO;
        private BigDecimal totalCartDiscount = BigDecimal.ZERO;
        private BigDecimal originalPrice = BigDecimal.ZERO;
        private BigDecimal discountedPrice = BigDecimal.ZERO;
        private BigDecimal discountedSubtotal = BigDecimal.ZERO;

        private int maximumRefundableQuantity;

        private RefundLineItem.RestockType restockType = RefundLineItem.RestockType.no_restock;

        private Boolean removal;

        @JsonIgnore
        private org.example.order.order.domain.order.model.LineItem lineItem;

        public LineItem copy() {
            return new LineItem(
                    quantity, lineItemId, locationId,
                    price, subtotal, totalTax, totalCartDiscount, originalPrice,
                    discountedPrice, discountedSubtotal, maximumRefundableQuantity,
                    restockType, removal, lineItem
            );
        }
    }
}
