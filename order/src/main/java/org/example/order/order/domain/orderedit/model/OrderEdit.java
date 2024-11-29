package org.example.order.order.domain.orderedit.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.base.Preconditions;
import jakarta.persistence.*;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.service.orderedit.OrderEditContextService;
import org.example.order.order.application.service.orderedit.OrderEditRequest;
import org.example.order.order.application.utils.TaxSetting;
import org.example.order.order.application.utils.TaxSettingValue;
import org.example.order.order.domain.order.model.OrderId;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Getter
@DynamicUpdate
@Table(name = "order_edits")
public class OrderEdit extends AggregateRoot<OrderEdit> {

    @EmbeddedId
    @JsonUnwrapped
    @AttributeOverride(name = "storeId", column = @Column(name = "store_id"))
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    private OrderEditId id;

    @Min(1)
    private int orderId;

    private Currency currency;

    @Min(0)
    private int orderVersion;

    @Min(0)
    @NotNull
    private BigDecimal subtotalLineItemQuantity;

    @Min(0)
    @NotNull
    private BigDecimal subtotalPrice;

    @Min(0)
    private BigDecimal cartDiscountAmount;

    @Min(0)
    @NotNull
    private BigDecimal totalPrice;

    @Min(0)
    @NotNull
    private BigDecimal totalOutStanding;

    private boolean committed;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid AddedLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid AddedTaxLine> taxLines = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid AddedDiscountApplication> discountApplications = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid AddedDiscountAllocation> discountAllocations = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid OrderStagedChange> stagedChanges = new ArrayList<>();

    @NotNull
    private Instant createdAt;
    private Instant modifiedAt;
    private Instant committedAt;

    @Version
    private int version;

    protected OrderEdit() {
    }

    public OrderEdit(
            OrderId orderId,
            Currency currency,
            BigDecimal subtotalLineItemQuantity,
            BigDecimal subtotalPrice,
            BigDecimal cartDiscountAmount,
            BigDecimal totalPrice,
            BigDecimal totalOutStanding
    ) {
        this.id = new OrderEditId(orderId.getStoreId(), 1);

        this.orderId = orderId.getId();
        this.orderVersion = 1;

        this.currency = currency;

        this.subtotalLineItemQuantity = subtotalLineItemQuantity;
        this.subtotalPrice = subtotalPrice;
        this.cartDiscountAmount = cartDiscountAmount;
        this.totalPrice = totalPrice;
        this.totalOutStanding = totalOutStanding;

        this.committed = false;
        this.version = 1;

        this.createdAt = Instant.now();
        this.modifiedAt = Instant.now();
    }

    /**
     * add newLineItem
     * B1: tính toán lại quantity
     * B2: tính lại price
     * B3: xác định xem có nên apply thuế không => add thuế
     * B4: xác định changeType, action => add vảo
     * B5: chỉ xử lý case có productId => apply tax
     */
    public void addLineItem(AddedLineItem lineItem, TaxSetting taxSetting) {
        lineItem.setAggRoot(this);

        this.lineItems.add(lineItem);
        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity.add(lineItem.getEditableQuantity());

        OrderStagedChange.ChangeType type;
        OrderStagedChange.BaseAction action;
        if (lineItem.getVariantId() != null) { // add variant
            type = OrderStagedChange.ChangeType.add_variant;
            action = OrderStagedChange.AddVariant.builder()
                    .lineItemId(lineItem.getId())
                    .variantId(lineItem.getVariantId())
                    .locationId(lineItem.getLocationId())
                    .quantity(lineItem.getEditableQuantity())
                    .build();
        } else { // add custom item
            type = OrderStagedChange.ChangeType.add_custom_item;
            action = OrderStagedChange.AddCustomItem.builder()
                    .lineItemId(lineItem.getId())
                    .title(lineItem.getTitle())
                    .price(lineItem.getOriginalUnitPrice())
                    .quantity(lineItem.getEditableQuantity())
                    .taxable(lineItem.isTaxable())
                    .requireShipping(lineItem.isRequireShipping())
                    .locationId(lineItem.getLocationId())
                    .build();
        }

        this.adjustPrice(lineItem.getOriginalUnitPrice());

        this.calculateTax(lineItem, taxSetting);

        var stagedChange = new OrderStagedChange(UUID.randomUUID(), type, action);
        stagedChange.setAggRoot(this);
        this.stagedChanges.add(stagedChange);
    }

    private void calculateTax(AddedLineItem lineItem, TaxSetting taxSetting) {
        if (CollectionUtils.isEmpty(taxSetting.getTaxes())) {
            return;
        }

        var taxValueMap = taxSetting.getTaxes().stream()
                .collect(Collectors.toMap(TaxSettingValue::getProductId,
                        Function.identity()));
        Integer productId = lineItem.getProductId();
        var taxValue = taxValueMap.get(productId);
        if (taxValue == null) return;

        this.addTax(taxValue, lineItem, taxSetting.isTaxIncluded());
    }

    private void addTax(TaxSettingValue taxValue, AddedLineItem lineItem, boolean taxIncluded) {
        var currency = getEditCurrency();
        var taxLine = new AddedTaxLine(
                UUID.randomUUID(),
                taxValue.getTitle(),
                taxValue.getRate(),
                lineItem,
                currency,
                taxIncluded
        );
        taxLine.setAggRoot(this);
        this.taxLines.add(taxLine);

        this.adjustTaxPrice(taxLine.getPrice(), taxIncluded);
    }

    private void adjustTaxPrice(BigDecimal adjustmentPrice, boolean taxIncluded) {
        if (!taxIncluded) {
            totalPrice = totalPrice.add(adjustmentPrice);
            totalOutStanding = totalOutStanding.add(adjustmentPrice);
        }
    }

    private Currency getEditCurrency() {
        if (this.currency != null) return this.currency;
        this.currency = Currency.getInstance("VND");
        return this.currency;
    }


    private void adjustPrice(BigDecimal adjustmentPrice) {
        this.subtotalPrice = this.subtotalPrice.add(adjustmentPrice);
        this.totalPrice = this.totalPrice.add(adjustmentPrice);
        this.totalOutStanding = this.totalOutStanding.add(adjustmentPrice);
    }

    /**
     * TH1: increase addedLineItem =>
     * - tăng quantity => tính lại price trong lineItem, trong orderEdit (nếu có discount)
     * - add taxLine nếu có
     * TH2: increase lineItem =>
     */
    public void increaseLineItemQuantity(OrderEditContextService.IncrementContext context, OrderEditRequest.Increment increment) {
        var lineItemInfo = context.lineItemInfo();
        var taxSetting = context.taxSetting();

        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity.add(BigDecimal.valueOf(increment.getDelta()));

        // increase added line item
        if (lineItemInfo.addedLineItem() != null) {
            var addedLine = lineItemInfo.addedLineItem();

            var adjustmentPrice = addedLine.adjustQuantity(increment.getDelta());

            this.adjustPrice(adjustmentPrice);

            var taxLine = this.taxLines.stream()
                    .filter(tax -> Objects.equals(tax.getLineItemId(), increment.getLineItemId()))
                    .findFirst()
                    .orElse(null);
            if (taxLine != null) {
                var adjustmentTaxPrice = taxLine.adjustQuantity(
                        addedLine.getEditableQuantity(),
                        addedLine.getEditableSubtotal(),
                        getEditCurrency(),
                        taxSetting.isTaxIncluded());
                adjustTaxPrice(adjustmentTaxPrice, taxSetting.isTaxIncluded());
            }
            return;
        }

        var lineItem = lineItemInfo.lineItem();
        Preconditions.checkNotNull(lineItem);

        if (CollectionUtils.isNotEmpty(lineItem.getDiscountAllocations())) {
            throw new ConstrainViolationException(
                    "line_item",
                    "discounted_line_item cannot adjust quantity"
            );
        }

        var adjustQuantity = BigDecimal.valueOf(increment.getDelta());
        var adjustmentPrice = lineItem.getDiscountUnitPrice().multiply(adjustQuantity);

        this.adjustPrice(adjustmentPrice);

        this.removeStagedChangeFor(increment.getLineItemId());
    }

    private void removeStagedChangeFor(String lineItemId) {

    }
}
