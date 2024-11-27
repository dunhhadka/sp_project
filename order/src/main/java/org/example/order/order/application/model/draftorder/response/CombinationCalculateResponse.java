package org.example.order.order.application.model.draftorder.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.example.order.order.domain.draftorder.model.VariantType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CombinationCalculateResponse {
    private List<LineItem> lineItems;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class LineItem {
        private Long variantId;
        private Long productId;

        private String token;
        private String title;
        private String variantTitle;
        private String unit;
        private String itemUnit;
        private String sku;

        private boolean taxable;
        private boolean requiresShipping;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Integer grams;
        @Builder.Default
        private BigDecimal quantity = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal price = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal linePrice = BigDecimal.ZERO;

        @Builder.Default
        private List<LineItemComponent> components = new ArrayList<>();
        @Builder.Default
        private List<ComboPacksizeDiscountAllocation> discountAllocations = new ArrayList<>();
        @Builder.Default
        private List<ComboPacksizeTaxLine> taxLines = new ArrayList<>();
        @Builder.Default
        private VariantType type = VariantType.normal;

        @Builder.Default
        private Map<String, Object> metaData = new HashMap<>();
    }
}
