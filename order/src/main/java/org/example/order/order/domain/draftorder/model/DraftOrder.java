package org.example.order.order.domain.draftorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.utils.*;
import org.example.order.order.domain.draftorder.persistence.NumberGenerator;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Entity
@Getter
@NoArgsConstructor
@DynamicUpdate
@Table(name = "draft_orders")
public class DraftOrder extends AggregateRoot<DraftOrder> {

    @JsonIgnore
    @Transient
    private TaxHelper taxHelper;

    @Transient
    private TaxSetting taxSetting;

    @EmbeddedId
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    @AttributeOverride(name = "storeId", column = @Column(name = "store_id"))
    @JsonUnwrapped
    private DraftOrderId id;

    @NotBlank
    @Size(max = 50)
    private String name;

    @Min(0)
    private Integer copyOrderId; // id gốc của order khi sao chép

    private Integer userId;

    private Instant createdOn;
    private Instant modifiedOn;

    @Embedded
    @JsonUnwrapped
    private @Valid DraftOrderPricingInfo pricingInfo;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private Status status = Status.open;

    private Instant completedOn;

    private Integer orderId;

    private Integer customerId;

    private boolean taxesIncluded;

    @Embedded
    @JsonUnwrapped
    private @Valid DraftOrderInfo draftOrderInfo;

    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "draft_order_tags", joinColumns = {
            @JoinColumn(name = "draft_order_id", referencedColumnName = "id"),
            @JoinColumn(name = "store_id", referencedColumnName = "store_id")
    })
    private List<DraftOrderTag> tags;

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private DraftOrderAddress shippingAddress;

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private DraftOrderAddress billingAddress;

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private DraftOrderShippingLine shippingLine;

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private DraftAppliedDiscount appliedDiscount;

    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    @Min(1)
    private List<@Valid DraftOrderLineItem> lineItems;

    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private List<@Valid DraftProperty> noteAttributes = new ArrayList<>();

    @Setter
    private Integer modifiedId;

    private BigDecimal grams;

    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private List<@Valid DraftDiscountApplication> discountApplications = new ArrayList<>();

    public DraftOrder(
            DraftOrderId id,
            NumberGenerator numberGenerator,
            Integer copyOrderId,
            TaxHelper taxHelper,
            Currency currency
    ) {
        this.id = id;
        this.name = "#D" + numberGenerator.generateDraftNumber(id.getStoreId());
        this.copyOrderId = copyOrderId;
        this.taxHelper = taxHelper;
        this.status = Status.open;
        this.draftOrderInfo = DraftOrderInfo.builder()
                .currency(currency)
                .build();
        this.createdOn = this.modifiedOn = Instant.now();
    }

    public void setLineItems(List<DraftOrderLineItem> lineItems) {
        if (allowEdit()) {
            this.lineItems = lineItems;
            this.lineItems.forEach(line -> line.setAggRoot(this));
            this.calculateWeight();
            this.modifiedOn = Instant.now();
        }
    }

    private BigDecimal getTotalShippingPrice() {
        return this.shippingLine != null ? this.shippingLine.getPrice() : BigDecimal.ZERO;
    }

    private BigDecimal getTotalLineItemPrice() {
        if (CollectionUtils.isEmpty(this.lineItems)) return BigDecimal.ZERO;
        return this.lineItems.stream().map(DraftOrderLineItem::getTotalOriginal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getLineItemSubtotalPrice() {
        if (CollectionUtils.isEmpty(this.lineItems)) return BigDecimal.ZERO;
        return this.lineItems.stream().map(DraftOrderLineItem::getDiscountedTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean shouldCalculateTax(DraftOrderInfo draftOrderInfo, TaxSetting taxSetting) {
        if (draftOrderInfo.getTaxExempt() != null) {
            return !draftOrderInfo.getTaxExempt();
        } else {
            return taxSetting.getStatus() == TaxSetting.TaxStatus.active;
        }
    }

    private TaxSetting getTaxSetting() {
        var countryCode = this.billingAddress != null ? this.billingAddress.getCountryCode() : null;
        if (StringUtils.isBlank(countryCode)) countryCode = "VN";
        if (!"VN".equals(countryCode)) return TaxSetting.defaultTax();

        List<Integer> productIds = new ArrayList<>();
        for (var lineItem : lineItems) {
            var productId = lineItem.getProductInfo().getProductId();
            if (productId == null) continue;
            productIds.add(productId);
            if (CollectionUtils.isNotEmpty(lineItem.getComponents())) {
                var productComponentIds = lineItem.getComponents().stream()
                        .map(DraftLineItemComponent::getProductId)
                        .filter(NumberUtils::isPositive)
                        .distinct().toList();
                productIds.addAll(productComponentIds);
            }
        }

        if (this.lineItems.stream().anyMatch(line -> line.isCustom() || !NumberUtils.isPositive(line.getProductInfo().getProductId()))) {
            productIds.add(0);
        }

        var validProductIds = new HashSet<>(productIds);

        if (taxSetting != null
                && countryCode.equals(this.taxSetting.getCountryCode())
                && CollectionUtils.isEqualCollection(taxSetting.getProductIds(), validProductIds)) {
            return this.taxSetting;
        }

        this.taxSetting = taxHelper.getTaxSetting(this.id.getStoreId(), countryCode, validProductIds);
        return this.taxSetting;
    }

    private void calculateWeight() {
        if (CollectionUtils.isEmpty(this.lineItems)) {
            this.grams = BigDecimal.ZERO;
            return;
        }
        this.grams = this.lineItems.stream()
                .map(DraftOrderLineItem::getGrams)
                .filter(NumberUtils::isPositive)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean allowEdit() {
        return this.status != Status.complete;
    }

    public void overrideTags(List<String> newTags) {
        if (newTags == null) newTags = new ArrayList<>();

        var currentTags = this.tags.stream().map(DraftOrderTag::getValue).sorted().toList();
        newTags = newTags.stream().sorted().toList();
        if (CollectionUtils.isEqualCollection(currentTags, newTags)) return;

        for (var tag : this.tags) {
            if (!newTags.contains(tag.getValue())) internalRemoveTag(tag);
        }

        newTags.forEach(this::internalAddTag);

        this.reorderTags();
    }

    private void reorderTags() {
        if (CollectionUtils.isEmpty(this.tags)) return;
        this.tags = this.tags.stream().sorted(this.getTagComparator()).toList();
    }

    private Comparator<DraftOrderTag> getTagComparator() {
        return Comparator.comparing(DraftOrderTag::getValue);
    }

    private void internalAddTag(String newTag) {
        if (this.tags.stream().anyMatch(tag -> StringUtils.equals(tag.getValue(), newTag))) return;

        var tag = new DraftOrderTag(newTag);
        this.tags.add(tag);
    }

    private void internalRemoveTag(DraftOrderTag tag) {
        if (CollectionUtils.isEmpty(this.tags) || !this.tags.contains(tag)) return;
        this.tags.remove(tag);
    }

    public void setDraftOrderInfo(DraftOrderInfo draftOrderInfo) {
        if (allowEdit()) {
            this.draftOrderInfo = draftOrderInfo;
            this.modifiedOn = Instant.now();
        }
    }


    public enum Status {
        open, complete
    }
}
