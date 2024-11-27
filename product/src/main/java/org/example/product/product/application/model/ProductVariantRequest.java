package org.example.product.product.application.model;

import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.Variant;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductVariantRequest {
    private Integer id;
    private @Size(max = 50) String barcode;
    private @Size(max = 50) String sku;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String option1;
    private String option2;
    private String option3;
    private boolean taxable;
    private String inventoryManagement;
    private String inventoryPolicy;
    private Integer inventoryQuantity;
    private Integer oldInventoryQuantity;
    private String inventoryQuantityAdjustment;
    private boolean requireShipping;
    private Double weight;
    private String weightUnit;
    private String unit;
    private List<Integer> imagePosition;
    private List<@Valid InventoryQuantityRequest> inventoryQuantities;
    private Variant.VariantType type;
}
