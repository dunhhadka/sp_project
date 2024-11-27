package org.example.order.order.domain.draftorder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DraftProductInfo {
    private Integer variantId;
    private Long inventoryItemId;
    private String inventoryManagement;
    private Integer productId;
    @NotBlank
    private String title;
    private String variantTitle;
    private BigDecimal price;
    private String sku;
    private boolean taxable; // có thể apply tax hay không?
    private String vendor;
    private String unit;
    private String itemUnit;
    private VariantType type = VariantType.normal;
}
