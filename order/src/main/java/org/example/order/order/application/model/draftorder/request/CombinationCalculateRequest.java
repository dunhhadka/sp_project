package org.example.order.order.application.model.draftorder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.example.order.order.application.service.draftorder.CombinationCalculateResponse;
import org.example.order.order.domain.draftorder.model.VariantType;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class CombinationCalculateRequest { // dùng để tính toán lại
    @JsonProperty("update-product-info")
    private boolean updateProductInfo;
    @JsonProperty("calculate_tax")
    private boolean calculateTax;
    @JsonProperty("tax_exempt")
    private boolean taxExempt;
    @JsonProperty("taxes_included")
    private boolean taxIncluded;

    private @NotEmpty List<@Valid LineItem> lineItems;

    @Builder.Default
    private String currency = "VND";

    @Builder.Default
    private String countryCode = "VN";

    @Builder.Default
    private CalculateType calculateType = CalculateType.normal;

    public enum CalculateType {
        normal,
        calculate_and_split
    }

    @Getter
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
        private boolean requireShipping;

        private BigDecimal quantity;

        private Integer grams;

        private @NotNull @Positive BigDecimal price;
        @Builder.Default
        private BigDecimal linePrice = BigDecimal.ZERO;

        @Builder.Default
        private List<CombinationCalculateResponse.@Valid ComboPacksizeDiscountAllocation> discountAllocations = new ArrayList<>();

        private List<@Valid LineItemComponent> components;

        private VariantType type;
    }

    @Getter
    @Builder
    public static class LineItemComponent {
        private int variantId;
        private int productId;

        private Long inventoryItemId;

        private String sku;
        private String title;
        private String variantTitle;
        private String vendor;
        private String unit;
        private String inventoryManagement;
        private String inventoryPolicy;

        private int grams;

        private boolean taxable;
        private boolean requiresShipping;

        private BigDecimal quantity;
        private BigDecimal baseQuantity; // Số lượng default của mỗi thành phần của combo

        @Builder.Default
        private BigDecimal price = BigDecimal.ZERO;
        private @NotNull BigDecimal linePrice;

        private List<CombinationCalculateResponse.@Valid ComboPacksizeDiscountAllocation> discountAllocations;
        private boolean canBeOdd;
    }
}