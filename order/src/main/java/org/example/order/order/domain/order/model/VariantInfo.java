package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VariantInfo {
    private Integer productId;
    private Integer variantId;
    private boolean productExisted;

    @Setter(AccessLevel.PACKAGE)
    private @NotBlank @Size(max = 2000) String name;
    private @NotBlank @Size(max = 500) String title;
    private @Size(max = 1500) String variantTitle;
    private @Size(max = 255) String vendor;
    private @Size(max = 50) String sku;

    private @Min(0) int grams;
    private boolean requireShipping;

    private String variantInventoryManagement;
    private boolean restockable;

    private Long inventoryItemId;
    private @Size(max = 50) String unit;

    public enum VariantType {
        normal,
        combo,
        packsize
    }
}
