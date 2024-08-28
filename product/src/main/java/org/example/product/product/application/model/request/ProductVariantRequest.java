package org.example.product.product.application.model.request;

import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.example.product.product.application.annotation.StringInList;
import org.example.product.product.domain.product.model.Variant;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@JsonRootName("variant")
public class ProductVariantRequest {
    private Integer id;
    private @Size(max = 50) String barcode;
    private @Size(max = 50) String sku;
    private @DecimalMin(value = "0") @DecimalMax(value = "10000000000") BigDecimal price;
    private @DecimalMin(value = "0") @DecimalMax(value = "10000000000") BigDecimal compareAtPrice;
    private @Size(max = 500) String option1;
    private @Size(max = 500) String option2;
    private @Size(max = 500) String option3;
    private Boolean taxable;

    private @StringInList(array = {"thanhhoa", "bizweb"}, allowBlank = true) String inventoryManagement; // quản lý hàng tồn kho tại??
    private @Min(-1000000) @Max(1000000) Integer inventoryQuantity;
    private @Min(-1000000) @Max(1000000) Integer oldInventoryQuantity;
    private Integer quantityAdjustable;
    private Boolean requireShipping;

    private @NotNull @Min(0) @Max(2000000) Double weight;
    private @StringInList(array = {"kg", "g"}) String weightUnit;
    private @Size(max = 50) String unit;

    private Integer imagePosition;
    private Integer imageId;

    // thông tin để tạo inventoryLevel
    private @Size(max = 100) List<InventoryQuantityRequest> inventoryQuantities;
    private Variant.VariantType type;
}
