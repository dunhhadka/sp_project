package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.utils.NumberUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Where;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Entity
@Getter
@Table(name = "line_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LineItem {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    private Order aggRoot;

    @Id
    private int id;

    private @Min(1) int quantity;

    private @NotNull @Min(0) BigDecimal price;

    // giảm giá trên 1 quantity
    private @Min(0) BigDecimal totalDiscount;

    private @Size(max = 255) String discountCode;

    private int fulfillableQuantity;

    @Enumerated(value = EnumType.STRING)
    private FulfillmentStatus fulfillmentStatus;

    @JsonUnwrapped
    @Embedded
    private @Valid VariantInfo variantInfo;

    @Size(max = 100)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @JoinColumn(name = "targetId", referencedColumnName = "id", updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Where(clause = "targetType = 'line_item")
    @OrderBy("id asc")
    private List<@Valid DiscountAllocation> discountAllocations = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "targetId", referencedColumnName = "id", updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Where(clause = "targetType = 'line_item'")
    @OrderBy("id asc")
    private List<@Valid TaxLine> taxLines = new ArrayList<>();

    private boolean taxable;

    // giá sau discount /  1 quantity
    private BigDecimal discountUnitPrice;

    private BigDecimal discountedTotal;

    private BigDecimal originalTotal;

    private int currentQuantity;

    private int nonFulfillableQuantity;

    private int refundableQuantity;

    @Version
    private Integer version;

    private String combinationLineKey;

    private String fulfillmentService;

    public LineItem(
            int id,
            int quantity,
            BigDecimal price,
            BigDecimal discount,
            String discountCode,
            VariantInfo variantInfo,
            Boolean taxable,
            List<TaxLine> taxLines,
            String fulfillmentService,
            boolean giffCard,
            String combinationLineKey
    ) {
        this.id = id;
        this.price = price;
        this.quantity = quantity;
        this.fulfillableQuantity = quantity;

        this.totalDiscount = calculateDiscountAmount(discount, price);
        this.discountCode = discountCode;

        this.internalSetVariantInfo(variantInfo);
        this.combinationLineKey = combinationLineKey;

        this.taxable = taxable;
        this.applyTax(taxLines);

        this.currentQuantity = this.quantity;
        this.refundableQuantity = quantity;
        this.fulfillmentService = fulfillmentService;
        this.nonFulfillableQuantity = 0;

        this.originalTotal = this.price.multiply(BigDecimal.valueOf(this.quantity));
        this.discountedTotal = this.originalTotal;
        this.discountUnitPrice = this.price.subtract(discount);
    }

    public void applyTax(List<TaxLine> taxLines) {
        this.taxLines.addAll(taxLines);
    }

    private void internalSetVariantInfo(VariantInfo variantInfo) {
        Objects.requireNonNull(variantInfo);
        StringBuilder nameBuilder = new StringBuilder(variantInfo.getTitle());
        String variantTitle = variantInfo.getVariantTitle();
        if (StringUtils.isNotBlank(variantTitle) && !StringUtils.equals(variantTitle, "Default Title")) {
            nameBuilder.append(" - ").append(variantTitle);
        }
        variantInfo.setName(nameBuilder.toString());
        this.variantInfo = variantInfo;
    }

    private BigDecimal calculateDiscountAmount(BigDecimal discount, BigDecimal price) {
        if (!NumberUtils.isPositive(discount)) return BigDecimal.ZERO;
        return discount.min(price);
    }

    public BigDecimal getSubtotalLinePrice() {
        return this.price.subtract(this.totalDiscount)
                .multiply(BigDecimal.valueOf(this.quantity));
    }

    public void allocateDiscount(DiscountAllocation discountAllocation) {
        this.discountAllocations.add(discountAllocation);
        this.calculateDiscount();
    }

    private void calculateDiscount() {
        var decimalQuantity = BigDecimal.valueOf(this.quantity);
        var lineItemDiscount = getProductDiscount();

        this.discountedTotal = this.price.multiply(decimalQuantity).subtract(lineItemDiscount);

        var currency = this.aggRoot.getMoneyInfo().getCurrency();

        this.discountUnitPrice = this.discountedTotal
                .divide(decimalQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
    }

    public BigDecimal getOrderDiscount() {
        return this.getDiscountAmountByDiscountClas(discount ->
                discount.getRuleType() == DiscountApplication.RuleType.order);
    }

    public BigDecimal getProductDiscount() {
        return this.getDiscountAmountByDiscountClas(discount ->
                discount.getRuleType() == DiscountApplication.RuleType.product);
    }

    private BigDecimal getDiscountAmountByDiscountClas(Predicate<DiscountApplication> rule) {
        if (!CollectionUtils.isEmpty(this.discountAllocations)) {
            var discountApplications = this.aggRoot.getDiscountApplications();
            if (!CollectionUtils.isEmpty(discountApplications)) {
                var discountAmount = BigDecimal.ZERO;
                for (var discountAllocation : this.discountAllocations) {
                    var discountApplication = discountApplications.get(discountAllocation.getApplicationIndex());
                    boolean satisfied = rule.test(discountApplication);
                    if (satisfied) {
                        discountAmount = discountAmount.add(discountAllocation.getAmount());
                    }
                }
                return discountAmount;
            }
        }
        return BigDecimal.ZERO;
    }

    public int getTotalWeight() {
        return this.quantity * this.variantInfo.getGrams();
    }

    public BigDecimal getTotalTax() {
        var totalTax = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(this.taxLines)) {
            totalTax = this.taxLines.stream()
                    .map(TaxLine::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return totalTax;
    }

    public void markAsFulfilled(int fulfilledQuantity) {
        this.fulfillableQuantity = this.fulfillableQuantity - fulfilledQuantity;
        this.updateFulfillmentStatus();
    }

    private void updateFulfillmentStatus() {
        if (this.fulfillableQuantity == 0) {
            this.fulfillmentStatus = FulfillmentStatus.fulfilled;
        } else if (this.fulfillableQuantity > 0 && this.quantity - this.nonFulfillableQuantity > this.fulfillableQuantity) {
            this.fulfillmentStatus = FulfillmentStatus.partial;
        } else if (this.fulfillmentStatus != FulfillmentStatus.restocked) {
            this.fulfillmentStatus = null;
        }
    }

    public BigDecimal getDiscountedPrice() {
        return this.price.subtract(totalDiscount);
    }

    public enum FulfillmentStatus {
        partial,
        fulfilled,
        restocked
    }
}
