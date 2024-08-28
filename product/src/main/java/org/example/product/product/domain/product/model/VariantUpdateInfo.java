package org.example.product.product.domain.product.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class VariantUpdateInfo {
    private VariantIdentityInfo identityInfo;
    private VariantPricingInfo pricingInfo;
    private VariantOptionInfo optionInfo;
    private VariantInventoryInfo inventoryInfo;
    private VariantPhysicalInfo physicalInfo;

    private Integer imageId;
}
