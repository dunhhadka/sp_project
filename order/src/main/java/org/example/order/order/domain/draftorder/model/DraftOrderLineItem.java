package org.example.order.order.domain.draftorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.utils.BigDecimals;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.TaxLineUtils;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

@Getter
@NoArgsConstructor
public class DraftOrderLineItem {
    @Setter
    @JsonIgnore
    private DraftOrder aggRoot;

    private boolean custom;

    @JsonUnwrapped
    @Valid
    private DraftProductInfo productInfo;

    private List<@Valid DraftTaxLine> taxLines = new ArrayList<>();

    private @Min(1) int quantity;
    private @Min(0) int grams;
    private boolean requireShipping;

    @Valid
    private DraftAppliedDiscount appliedDiscount = new DraftAppliedDiscount();

    private List<@Valid DraftProperty> properties = new ArrayList<>();

    /**
     * Giá của một đơn vị sản phẩm sau khi trừ đi khuyến mãi trong lineItem = lineItem.price - round(lineItem.applied_discount.amount/lineItem.quantity)
     */
    private BigDecimal discountedUnitPrice;

    /**
     * Tỉ lệ phân bổ (KM ĐH) = discounted_total/sum(discounted_total)
     */
    private BigDecimal allocationRatio = BigDecimal.ZERO;

    private BigDecimal discountOrder = BigDecimal.ZERO;

    private List<DraftLineItemComponent> components = new ArrayList<>();

    private List<DraftDiscountAllocation> discountAllocations = new ArrayList<>();

    @JsonIgnore
    private Currency currency;

    public DraftOrderLineItem(
            boolean custom,
            DraftProductInfo productInfo,
            int quantity,
            int grams,
            boolean requireShipping,
            DraftAppliedDiscount appliedDiscount,
            List<DraftProperty> properties,
            Currency currency
    ) {
        this.custom = custom;
        this.productInfo = productInfo;
        this.quantity = quantity;
        this.grams = grams;
        this.requireShipping = requireShipping;
        this.properties = properties;
        this.currency = currency;
        this.setAppliedDiscount(appliedDiscount);
    }

    private void setAppliedDiscount(DraftAppliedDiscount appliedDiscount) {
        if (appliedDiscount == null || !NumberUtils.isPositive(appliedDiscount.getValue())) {
            return;
        }
        var quantityDecimal = BigDecimal.valueOf(this.quantity);
        var amount = switch (appliedDiscount.getValueType()) {
            case fixed_amount -> appliedDiscount.getValue().multiply(quantityDecimal);
            case percentage -> this.productInfo.getPrice()
                    .multiply(appliedDiscount.getValue().min(BigDecimals.ONE_HUNDRED))
                    .divide(BigDecimals.ONE_HUNDRED, currency.getDefaultFractionDigits(), RoundingMode.FLOOR)
                    .multiply(quantityDecimal);
        };
        appliedDiscount.setAmount(amount.min(this.getTotalOriginal()));

        this.appliedDiscount = appliedDiscount;

        this.calculateDiscountedUnitPrice();
    }

    private void calculateDiscountedUnitPrice() {
        if (this.appliedDiscount == null) {
            this.discountedUnitPrice = this.productInfo.getPrice();
        }
        BigDecimal quantityDecimal = BigDecimal.valueOf(this.quantity);
        BigDecimal discountedTotal = this.getTotalOriginal().subtract(this.appliedDiscount.getAmount());
        this.discountedUnitPrice = discountedTotal
                .divide(quantityDecimal, this.currency.getDefaultFractionDigits(), RoundingMode.DOWN);
    }

    public BigDecimal getTotalOriginal() {
        if (!NumberUtils.isPositive(this.productInfo.getPrice())) return BigDecimal.ZERO;
        var quantityDecimal = BigDecimal.valueOf(this.quantity);
        return quantityDecimal.multiply(this.productInfo.getPrice());
    }

    public void setDiscountAllocations(List<DraftDiscountAllocation> discountAllocations) {
        if (CollectionUtils.isEmpty(discountAllocations)) return;
        this.discountAllocations = discountAllocations;
    }

    public BigDecimal getDiscountedTotalPrice() {
        if (this.appliedDiscount != null) return this.getTotalOriginal().subtract(this.appliedDiscount.getAmount());
        return this.getTotalOriginal();
    }

    public void addDiscount(BigDecimal allocateRatio, BigDecimal discountOrder, boolean taxesIncluded) {
        this.allocationRatio = allocateRatio;
        this.discountOrder = discountOrder;
        this.calculateTax(taxesIncluded);
    }

    private void calculateTax(boolean taxesIncluded) {
        if (CollectionUtils.isEmpty(taxLines) || this.productInfo == null || !this.productInfo.isTaxable()) {
            return;
        }
        var price = this.getTotalOriginal()
                .subtract(this.appliedDiscount != null ? this.appliedDiscount.getAmount() : BigDecimal.ZERO)
                .subtract(this.discountOrder != null ? this.discountOrder : BigDecimal.ZERO);
        for (var taxLine : taxLines) {
            var taxPrice = TaxLineUtils.calculatePrice(price, taxLine.getRate(), taxesIncluded, currency);
            taxLine.setPrice(taxPrice);
        }
    }

    public void setTaxLines(List<DraftTaxLine> taxLines, boolean taxesIncluded) {
        this.taxLines = taxLines;
        this.calculateTax(taxesIncluded);
    }

    public void removeTaxes() {
        this.taxLines = new ArrayList<>();
    }

    public void setMerTaxLines(List<DraftTaxLine> draftTaxLines) {
        this.taxLines = draftTaxLines;
    }
}
