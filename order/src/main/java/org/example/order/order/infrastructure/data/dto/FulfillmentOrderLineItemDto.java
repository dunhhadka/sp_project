package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class FulfillmentOrderLineItemDto {
    private long id;
    private int storeId;
    private long fulfillmentOrderId;
    private long orderId;
    private long lineItemId;
    private Long variantId;
    private Long productId;
    private Long inventoryItemId;
    private BigDecimal price;
    private BigDecimal discountedUnitPrice;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private String title;
    private String variantTitle;
    private String sku;
    private String vendor;
    private Integer grams;
    private Boolean requiresShipping;
    private Instant createdOn;
    private String createdBy;
    private Instant modifiedOn;
    private String lastModifiedBy;
}
