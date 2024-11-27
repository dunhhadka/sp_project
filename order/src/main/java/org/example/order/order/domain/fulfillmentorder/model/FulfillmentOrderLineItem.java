package org.example.order.order.domain.fulfillmentorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "fulfillment_order_line_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FulfillmentOrderLineItem {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "fulfillmentOrderId", referencedColumnName = "id")
    private FulfillmentOrder aggRoot;

    @Id
    private int id;
    private int orderId;
    private int lineItemId;
    private int inventoryItemId;
    private Integer variantId;

    @Embedded
    @JsonUnwrapped
    private ProductVariantInfo variantInfo;

    private Integer totalQuantity;
    private Integer remainingQuantity;

    public FulfillmentOrderLineItem(
            int id,
            int orderId,
            int lineItemId,
            Long inventoryItemId,
            Integer variantId,
            ProductVariantInfo productVariantInfo,
            int quantity
    ) {
        this.id = id;
        this.orderId = orderId;
        this.lineItemId = lineItemId;
        this.inventoryItemId = inventoryItemId == null ? 0 : inventoryItemId.intValue();
        this.variantId = variantId;
        this.variantInfo = productVariantInfo;
        this.totalQuantity = quantity;
        this.remainingQuantity = quantity;
    }

    public void fulfillAndClose() {
        this.remainingQuantity = 0;
    }


    public void restock(int quantity) {
        this.remainingQuantity -= quantity;
        this.totalQuantity -= quantity;
    }

    public void fulfillAll() {
        this.remainingQuantity = 0;
    }

    public void fulfillAndClose(int quantity) {
        this.remainingQuantity = 0;
    }

    public void fulfill(int quantity) {
        this.remainingQuantity -= quantity;
    }

    public void closeEntry() {
        this.totalQuantity -= remainingQuantity;
        this.remainingQuantity = 0;
    }
}
