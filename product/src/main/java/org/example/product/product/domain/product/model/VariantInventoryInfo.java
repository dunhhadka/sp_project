package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.product.application.annotation.StringInList;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VariantInventoryInfo {
    @StringInList(array = {"thanhhoa", "bizweb"}, allowBlank = true)
    private String inventoryManagement;

    @Min(-1000000)
    @Max(1000000)
    private Integer inventoryQuantity;
    @Min(-1000000)
    @Max(1000000)
    private Integer oldInventoryQuantity;

    private Integer quantityAdjustable;

    private Boolean requireShipping;
}
