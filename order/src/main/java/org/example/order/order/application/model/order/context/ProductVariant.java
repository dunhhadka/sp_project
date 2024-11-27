package org.example.order.order.application.model.order.context;

import lombok.Builder;
import lombok.Getter;
import org.example.order.order.domain.draftorder.model.VariantType;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class ProductVariant {
    private int id;
    private String barcode;
    private String sku;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String option1;
    private String option2;
    private String option3;
    private boolean taxable;
    private String inventoryManagement;
    private String inventoryPolicy;
    private int inventoryQuantity;
    private boolean requiresShipping;
    private double weight;
    private String weightUnit;
    private Integer imageId;
    private int position;
    private String title;
    private int grams;
    private int productId;
    private Instant createdOn;
    private Instant modifiedOn;
    private Integer inventoryItemId;
    private String status;
    // gán default để đẩy trước bên product
    @Builder.Default
    private VariantType type = VariantType.normal;
    private String unit;
}
