package org.example.order.order.domain.refund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.order.application.model.order.response.RefundCalculateResponse;
import org.example.order.order.domain.transaction.model.AbstractEnumConverter;
import org.example.order.order.domain.transaction.model.CustomValueEnum;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "refund_line_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundLineItem {

    @JsonIgnore
    @ManyToOne
    @JoinColumns({
//            @JoinColumn(name = "storeId", referencedColumnName = "storeId"),
//            @JoinColumn(name = "orderId", referencedColumnName = "orderId"),
            @JoinColumn(name = "refundId", referencedColumnName = "id")
    })
    private Refund refund;

    @Id
    private int id;

    private int lineItemId;

    private int quantity;

    private Integer locationId;

    private BigDecimal price;

    private BigDecimal subtotal;

    private BigDecimal totalTax = BigDecimal.ZERO;

    private BigDecimal totalCartDiscount;

    private boolean removal;

    @Convert(converter = RestockType.ValueConverter.class)
    private RestockType restockType;

    @Version
    private Integer version;

    public RefundLineItem(
            Integer id,
            RefundCalculateResponse.LineItem suggestedLineItem
    ) {
        this.id = id;
        this.quantity = suggestedLineItem.getQuantity();
        this.lineItemId = suggestedLineItem.getLineItemId();
        this.price = suggestedLineItem.getPrice();
        this.subtotal = suggestedLineItem.getSubtotal();
        this.totalTax = suggestedLineItem.getTotalTax();
        this.restockType = suggestedLineItem.getRestockType();
        this.removal = suggestedLineItem.getRemoval();
        this.locationId = suggestedLineItem.getLocationId().intValue();
        this.totalCartDiscount = suggestedLineItem.getTotalCartDiscount();
    }

    @JsonIgnore
    public BigDecimal getTotalOriginalPrice() {
        return this.price.multiply(BigDecimal.valueOf(this.quantity));
    }

    @Getter
    public enum RestockType implements CustomValueEnum<String> {
        no_restock("no_restock"),
        cancel("cancel"), // trong quá trình giao hàng => trả
        _return("return"), // khách nhận hàng rồi trả
        legacy_restock("legacy_restock");

        private final String value;

        RestockType(String value) {
            this.value = value;
        }

        public static class ValueConverter
                extends AbstractEnumConverter<RestockType>
                implements AttributeConverter<RestockType, String> {

            public ValueConverter() {
                super(RestockType.class);
            }
        }
    }
}
