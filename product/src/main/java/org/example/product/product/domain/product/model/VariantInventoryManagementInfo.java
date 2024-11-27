package org.example.product.product.domain.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;

import javax.validation.constraints.Size;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantInventoryManagementInfo extends ValueObject<VariantInventoryManagementInfo> {
    @Size(max = 20)
    @Builder.Default
    private String inventoryManagement = "bizweb";

    @Size(max = 20)
    @Builder.Default
    private String inventoryPolicy = "deny";
}
