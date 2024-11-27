package org.example.product.product.domain.product.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VariantUpdateInfo {
    private VariantOptionInfo optionInfo;
    private VariantIdentityInfo identityInfo;
    private VariantPricingInfo pricingInfo;
    private VariantInventoryManagementInfo inventoryManagementInfo;
    private VariantPhysicalInfo physicalInfo;
    private List<Integer> imageIds;
    private Integer inventoryQuantity;
    private Integer oldInventoryQuantity;
    private Integer inventoryQuantityAdjustment;
    private Integer position;
    private Variant.VariantType type;
}
