package org.example.order.order.application.service.draftorder;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.model.draftorder.request.CombinationCalculateRequest;
import org.example.order.order.domain.draftorder.model.VariantType;
import org.example.order.order.domain.order.model.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class CombinationCalculateResponse {

    private List<LineItem> lineItems;
    private Order order;

    @Getter
    @Setter
    public static class LineItem {
        private Integer variantId;
        private Integer productId;

        private String token;
        private String title;
        private String variantTitle;
        private String unit;
        private String itemUnit;
        private String sku;

        private boolean taxable;
        private boolean requireShipping;

        private Integer grams;
        private BigDecimal price;
        private BigDecimal quantity;
        private BigDecimal linePrice;

        private List<LineItemComponent> components;

        private List<ComboPacksizeDiscountAllocation> discountAllocations;

        private List<ComboPacksizeTaxLine> taxLines;

        private VariantType type;

        private Map<String, Object> metaData;
    }

    @Getter
    @Setter
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
        private boolean requireShipping;

        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal linePrice;
        private BigDecimal baseQuantity;

        private BigDecimal subtotal;

        private List<ComboPacksizeDiscountAllocation> discountAllocations;

        private List<ComboPacksizeTaxLine> taxLines;

        private VariantType type;

        private BigDecimal remainder;

        private ComboPacksizeDiscountAllocation remainderDiscountAllocation;

        private boolean canBeOdd;

        private boolean changed;

        public void addDiscountAllocation(ComboPacksizeDiscountAllocation allocation) {
            if (this.discountAllocations == null) this.discountAllocations = new ArrayList<>();
            this.discountAllocations.add(allocation);
            this.subtotal = this.subtotal.subtract(allocation.getAmount());
        }

        public void addTaxLine(ComboPacksizeTaxLine customTaxLine) {

        }
    }

    @Getter
    @Setter
    @Builder(toBuilder = true)
    public static class ComboPacksizeDiscountAllocation {
        private BigDecimal amount;
        private int discountApplicationIndex;

        private BigDecimal remainder;

        public void addAmount(BigDecimal addPrice) {
            this.amount = this.amount.add(addPrice);
        }
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
}
