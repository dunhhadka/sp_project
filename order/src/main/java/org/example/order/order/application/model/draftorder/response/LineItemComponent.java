package org.example.order.order.application.model.draftorder.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.model.draftorder.request.CombinationCalculateRequest;
import org.example.order.order.domain.draftorder.model.VariantType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Setter
public class LineItemComponent {
    private long variantId;
    private long productId;
    private Integer inventoryItemId;
    private String sku;
    private String title;
    private String variantTitle;
    private String vendor;
    private String unit;
    private String inventoryManagement;
    private String inventoryPolicy;
    private int grams;
    private boolean requiresShipping;
    private boolean taxable;
    private BigDecimal quantity;
    // Số lượng sản phẩm thành phần trong mỗi combo
    private BigDecimal baseQuantity;
    // giá một sản phẩm
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;
    // giá của toàn line thành phần
    private BigDecimal linePrice;
    // Khuyến mại của thành phần được chia từ lineItem.DraftDiscountAllocations
    @Builder.Default
    private List<ComboPacksizeDiscountAllocation> discountAllocations = new ArrayList<>();
    @Builder.Default
    private List<ComboPacksizeTaxLine> taxLines = new ArrayList<>();
    private VariantType source;

    // phần dư của  khi chia cho số lượng
    @Builder.Default
    private BigDecimal remainder = BigDecimal.ZERO;
    @JsonIgnore
    private ComboPacksizeDiscountAllocation remainderDiscountAllocation;
    @JsonIgnore
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
    // đánh dấu line đã được chia phần dư
    @JsonIgnore
    @Builder.Default
    private boolean changed = false;
    // đánh dâu line có thể bị lẻ
    @Builder.Default
    private boolean canBeOdd = false;

    public void addDiscountAllocations(CombinationCalculateRequest.ComboPacksizeDiscountAllocation discountAllocation) {
        if (this.discountAllocations == null) discountAllocations = new ArrayList<>();
//        this.discountAllocations.add(discountAllocation);
        this.subtotal = this.subtotal.subtract(discountAllocation.getAmount());
    }
}
