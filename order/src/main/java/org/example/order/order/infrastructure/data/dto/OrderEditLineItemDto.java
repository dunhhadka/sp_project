package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderEditLineItemDto {
    private UUID id;
    private int storeId;
    private int editingId;

    private Long variantId;

    private String sku;
    private String title;
    private String variantTitle;

    private boolean taxable;
    private boolean requiresShipping;

    private boolean restockable;

    private int editableQuantity;
    private BigDecimal editableSubtotal;

    private BigDecimal originalUnitPrice;
    private BigDecimal discountedUnitPrice;

    private boolean hasStagedDiscount;

    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;
}
