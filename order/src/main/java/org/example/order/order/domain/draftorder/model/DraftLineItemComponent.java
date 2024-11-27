package org.example.order.order.domain.draftorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class DraftLineItemComponent {

    @JsonIgnore
    private DraftOrder aggRoot;

    private int variantId;
    private int productId;
    private int grams;

    private boolean taxable;
    private boolean requireShipping;

    private Long inventoryItemId;

    private String inventoryManagement;
    private String inventoryPolicy;

    private String title;
    private String variantTitle;
    private String sku;
    private String unit;
    private String vendor;

    private BigDecimal quantity;

    private BigDecimal baseQuantity;
    private BigDecimal linePrice;
    private BigDecimal price;

    @Builder.Default
    private List<DraftDiscountAllocation> discountAllocations = new ArrayList<>();

    @Builder.Default
    private List<DraftTaxLine> taxLines = new ArrayList<>();

    public BigDecimal getDiscountedTotal() {
        var totalDiscount = getTotalDiscount();
        var totalOriginalPrice = getTotalOriginalPrice();
        return totalOriginalPrice.subtract(totalDiscount);
    }

    private BigDecimal getTotalOriginalPrice() {
        if (this.price == null || this.quantity == null) return BigDecimal.ZERO;
        return this.price.multiply(quantity);
    }

    private BigDecimal getTotalDiscount() {
        if (CollectionUtils.isEmpty(this.discountAllocations)) return BigDecimal.ZERO;
        return discountAllocations.stream()
                .map(DraftDiscountAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void setTaxLines(List<DraftTaxLine> taxLines) {
        if (CollectionUtils.isEmpty(taxLines)) return;
        this.taxLines = taxLines;
    }

    public void removeOrderDiscountAllocation() {
        if (this.aggRoot == null
                || CollectionUtils.isEmpty(this.discountAllocations)
                || CollectionUtils.isEmpty(this.aggRoot.getDiscountApplications())) {
            return;
        }

        var discountApplications = this.aggRoot.getDiscountApplications();
        this.discountAllocations = this.discountAllocations.stream()
                .filter(item -> {
                    var discountApplication = discountApplications.get(item.getDiscountApplicationIndex());
                    return discountApplication.getTargetType() == DiscountApplication.TargetType.line_item;
                })
                .toList();
    }
}
