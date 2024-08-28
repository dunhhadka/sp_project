package org.example.order.order.application.model.order.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class LineItemResponse {
    private int id;
    private BigDecimal price;
    private BigDecimal totalDiscount;
    private String discountCode;
    private String fulfillmentStatus;
    private int fulfillableQuantity;
    private int quantity;
    private int currentQuantity;
    private int grams;
    private Integer productId;
    private Integer variantId;
    private Long inventoryItemId;
    private boolean productExists;
    private String variantInventoryManagement;
    private boolean requiresShipping;
    private String name;
    private String title;
    private String variantTitle;
    private String productTitle;
    private String sku;
    private String vendor;
    private boolean giftCard;

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private List<DiscountAllocationResponse> discountAllocations;

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private List<TaxLineResponse> taxLines;

    private boolean taxable;

    private int nonFulfillableQuantity;
    private int refundableQuantity;
    private boolean restockable;

    private BigDecimal discountedUnitPrice;
    private BigDecimal discountedTotal;
    private BigDecimal originalTotal;

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private ImageResponse image;

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private Long combinationLineId;
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private String combinationLineKey;
    private String unit;
}
