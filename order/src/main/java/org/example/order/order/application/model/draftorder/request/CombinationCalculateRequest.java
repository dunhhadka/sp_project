package org.example.order.order.application.model.draftorder.request;

import lombok.*;
import org.example.order.order.domain.draftorder.model.VariantType;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationCalculateRequest {
    private boolean updateProductInfo;
    private boolean calculateTax;
    private boolean taxExempt;
    private boolean taxIncluded;
    private @NotEmpty List<@Valid LineItem> lineItems;
    @Builder.Default
    private String currency = "VND";
    @Builder.Default
    private String countryCode = "VN";

    @Getter
    @Setter
    @Builder
    public static class LineItem {
        private Integer variantId;
        private Integer productId;
        private String token;
        private @Size(max = 320) String title;
        private @Size(max = 500) String variantTitle;
        private @Size(max = 50) String sku;
        private @Size(max = 500) String vendor;
        private @Size(max = 50) String unit;

        private boolean taxable;
        private boolean requiresShipping;

        private Integer grams;

        private @NotNull @Min(0) BigDecimal quantity;
        private @Min(0) BigDecimal price;

        @Builder.Default
        private BigDecimal linePrice = BigDecimal.ZERO;

        @Builder.Default
        private List<@Valid ComboPacksizeDiscountAllocation> discountAllocations = new ArrayList<>();

        @Builder.Default
        private List<@Valid LineItemComponent> components = new ArrayList<>();

        @Builder.Default
        private List<@Valid ComboPacksizeTaxLine> taxLines = new ArrayList<>();

        private List<@Valid CustomAttributeRequest> properties;

        @Builder.Default
        private VariantType type = VariantType.normal;
    }

    @Getter
    @Setter
    @Builder(toBuilder = true)
    public static class ComboPacksizeDiscountAllocation {
        private @NotNull @Min(0) BigDecimal amount;
        private @Min(0) int discountApplicationIndex;
        @Builder.Default
        private BigDecimal remainder = BigDecimal.ZERO;
    }

    @Getter
    @Setter
    @Builder
    public static class LineItemComponent {
        private @Min(0) int variantId;
        private @Min(0) int productId;
        private Long inventoryItemId;
        private String sku;
        private String title;
        private String variantTitle;
        private String vendor;
        private String unit;
        private String inventoryManagement;
        private String inventoryPolicy;
        private int grams;
        private boolean requireShipping;
        private boolean taxable;
        @DecimalMin(value = "0.001")
        private BigDecimal quantity;
        private BigDecimal baseQuantity;
        @Builder.Default
        private BigDecimal price = BigDecimal.ZERO;
        private BigDecimal linePrice;
        @Builder.Default
        private List<@Valid ComboPacksizeDiscountAllocation> discountAllocations = new ArrayList<>();
        private VariantType source;

        @Builder.Default
        private boolean canBeOdd = false;
    }

    @Getter
    @Setter
    @Builder
    public static class ComboPacksizeTaxLine {
        private BigDecimal rate;
        private BigDecimal price;
        private String title;
        @Builder.Default
        private boolean custom = false;
    }

    @Getter
    @Setter
    public static class CustomAttributeRequest {
        private @Size(max = 255) String name;
        private @Size(max = 255) String value;
    }
}
