package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.LineItem;

import java.math.BigDecimal;

@Getter
@Setter
public class LineItemDto {
    private int storeId;
    private int orderId;
    private int id;

    private int quantity;
    private BigDecimal price;
    private BigDecimal totalDiscount;
    private BigDecimal discountedTotal;
    private String discountCode;
    private int fulfillableQuantity;
    private LineItem.FulfillmentStatus fulfillmentStatus;

    private Integer productId;
    private Integer variantId;
    private boolean productExisted;
    private String name;
    private String title;
    private String variantTitle;
    private String vendor;
    private String sku;
    private int grams;
    private boolean requireShipping;

    private String variantInventoryManagement;
    private boolean restockable;

    private Long inventoryItemId;
    private String unit;

    private boolean taxable;

    private BigDecimal discountUnitPrice;

    private BigDecimal originalTotal;

    private int currentQuantity;

    private int nonFulfillableQuantity;

    private int refundableQuantity;

    private String combinationLineKey;

    private String fulfillmentService;

    private Integer version;
}
