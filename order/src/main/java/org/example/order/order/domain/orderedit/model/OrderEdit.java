package org.example.order.order.domain.orderedit.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.base.Preconditions;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.service.orderedit.LineItemUtils;
import org.example.order.order.application.service.orderedit.OrderEditContextService;
import org.example.order.order.application.service.orderedit.OrderEditRequest;
import org.example.order.order.application.utils.TaxSetting;
import org.example.order.order.application.utils.TaxSettingValue;
import org.example.order.order.domain.order.model.LineItem;
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
@Slf4j
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
    }

    /**
     * B1 : remove lineItem
     * B2 : adjust price
     * B3 : remove stagedChange
     * B4 : remove applyTax(adjust tax price)
     */
    public UUID removeLineItem(UUID lineItemId, TaxSetting taxSetting) {
        AddedLineItem lineItem = requireAddLineItem(lineItemId);

        lineItems.remove(lineItem);

        adjustPrice(lineItem.getEditableSubtotal().negate());

        this.removeApplyDiscountChanged(lineItemId);

        this.removeChange(lineItemId);

        taxLines.stream()
                .filter(line -> Objects.equals(line.getLineItemId(), lineItemId.toString()))
                .findFirst().ifPresent(taxLine -> {
                    taxLines.remove(taxLine);
                    adjustTaxPrice(taxLine.getPrice().negate(), taxSetting.isTaxIncluded());
                });

        return lineItemId;
    }

    private void removeChange(UUID lineItemId) {
        this.stagedChanges.removeIf(change -> {
            OrderStagedChange.BaseAction action = change.getAction();
            if (action instanceof OrderStagedChange.AddVariant addVariant) {
                return Objects.equals(addVariant.getLineItemId(), lineItemId);
            }
            if (action instanceof OrderStagedChange.AddCustomItem addCustomItem) {
                return Objects.equals(addCustomItem.getLineItemId(), lineItemId);
            }
            return false;
        });
    }

    private void removeApplyDiscountChanged(UUID lineItemId) {
        AddedDiscountAllocation allocation = this.discountAllocations.stream()
                .filter(discount -> Objects.equals(discount.getLineItemId(), lineItemId))
                .findFirst()
                .orElse(null);
        if (allocation == null) return;

        this.discountAllocations.remove(allocation);
        this.discountApplications.removeIf(application -> Objects.equals(application.getId().toString(), allocation.getApplicationId()));

        this.stagedChanges.removeIf(change -> {
            if (change.getAction() instanceof OrderStagedChange.AddItemDiscount addItemDiscount) {
                return Objects.equals(addItemDiscount.getLineItemId(), lineItemId);
            }
            return false;
        });
    }

    public UUID adjustAddedLineQuantity(UUID lineItemId, OrderEditRequest.SetItemQuantity request, TaxSetting taxSetting) {
        AddedLineItem lineItem = requireAddLineItem(lineItemId);

        BigDecimal adjustmentQuantity = BigDecimal.valueOf(request.getQuantity());
        if (adjustmentQuantity.compareTo(lineItem.getEditableQuantity()) == 0) {
            log.info("skipping adjust line item quantity");
            return lineItemId;
        }

        BigDecimal quantityBeforeChange = lineItem.getEditableQuantity();
        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity
                .subtract(quantityBeforeChange)
                .add(adjustmentQuantity);

        BigDecimal adjustPrice = lineItem.adjustQuantity(adjustmentQuantity);
        this.adjustPrice(adjustPrice);

        BigDecimal discountAmount = lineItem.getTotalDiscount();
        if (discountAmount.signum() != 0) {
            this.discountAllocations.stream()
                    .filter(discount -> Objects.equals(discount.getLineItemId(), lineItemId))
                    .findFirst()
                    .ifPresent(discount -> discount.adjustAmount(discountAmount));
        }

        this.recalculateTaxLine(lineItem, taxSetting);

        this.stagedChanges.forEach(change -> {
            OrderStagedChange.BaseAction action = change.getAction();
            if (action instanceof OrderStagedChange.AddVariant event) {
                var newEvent = event.toBuilder()
                        .quantity(adjustmentQuantity)
                        .build();
                change.updateEvent(newEvent);
            }
            if (action instanceof OrderStagedChange.AddCustomItem event) {
                var newEvent = event.toBuilder()
                        .quantity(adjustmentQuantity)
                        .build();
                change.updateEvent(newEvent);
            }
        });

        return lineItemId;
    }

    private void recalculateTaxLine(AddedLineItem lineItem, TaxSetting taxSetting) {
        AddedTaxLine taxLine = this.taxLines.stream()
                .filter(tax -> Objects.equals(tax.getLineItemId(), lineItem.getId().toString()))
                .findFirst()
                .orElse(null);
        if (taxLine == null) return;

        BigDecimal adjustmentTaxPrice = taxLine.adjustQuantity(
                lineItem.getEditableQuantity(),
                lineItem.getEditableSubtotal(),
                getEditCurrency(),
                taxSetting.isTaxIncluded());
        this.adjustTaxPrice(adjustmentTaxPrice, taxSetting.isTaxIncluded());
    }

    private AddedLineItem requireAddLineItem(UUID lineItemId) {
        return this.lineItems.stream()
                .filter(line -> Objects.equals(lineItemId, line.getId()))
                .findFirst()
                .orElseThrow(() -> new ConstrainViolationException(
                        "line_item",
                        "Line item not found"));
    }

    /**
     * TH1: nếu là resetItem quantity => xử lý bình thường => remove tất cả các change liên quan đến line (stagedChange, taxLine)
     * TH2: nếu là increase or decrease line item {
     * - Th1: nếu chưa có change => create new change (type, action)
     * - Th2: nếu đã có change => {
     * -    -C1: update change(update stagedChange, update TaxLine) => update price
     * -    -C2: remove change => insert lại từ đầu
     * }
     * }
     */
    public void recordQuantityAdjustment(
            LineItem lineItem,
            TaxSetting taxSetting,
            OrderEditRequest.SetItemQuantity request
    ) {
        BigDecimal currentQuantity = BigDecimal.valueOf(lineItem.getFulfillableQuantity());
        BigDecimal newQuantity = BigDecimal.valueOf(request.getQuantity());
        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity
                .subtract(currentQuantity)
                .add(newQuantity);

        List<OrderStagedChange> changes = LineItemUtils.getChanges(this.stagedChanges, lineItem.getId());
        if (changes.size() > 1) {
            throw new ConstrainViolationException(
                    "staged_change",
                    "greater than or equal two action for %s".formatted(lineItem.getId()));
        }

        if (newQuantity.compareTo(currentQuantity) == 0) {
            if (changes.isEmpty()) return;
            removeExistingLineItemChange(lineItem, taxSetting, changes.get(0));

            this.modifiedAt = Instant.now();

            return;
        }

        BigDecimal delta = newQuantity.subtract(currentQuantity);

        OrderStagedChange.ChangeType type;
        OrderStagedChange.BaseAction action;
        if (newQuantity.compareTo(currentQuantity) > 0) { // increase line item
            type = OrderStagedChange.ChangeType.increment_item;
            action = OrderStagedChange.IncrementItem.builder()
                    .lineItemId(lineItem.getId())
                    .delta(delta.intValue())
                    .locationId(request.getLocationId())
                    .build();
        } else {
            type = OrderStagedChange.ChangeType.decrement_item;
            action = OrderStagedChange.DecrementItem.builder()
                    .lineItemId(lineItem.getId())
                    .restock(request.isRestock())
                    .delta(delta.intValue())
                    .locationId(request.getLocationId())
                    .build();
        }

        if (changes.isEmpty()) {
            this.createChange(type, action);
            this.updatePrice(type, delta, lineItem.getDiscountUnitPrice(), BigDecimal.ZERO);
        } else {
            var change = changes.get(0);
            BigDecimal currentDelta;
            if (change.getAction() instanceof OrderStagedChange.IncrementItem incrementItem) {
                currentDelta = BigDecimal.valueOf(incrementItem.getDelta());
            } else if (change.getAction() instanceof OrderStagedChange.DecrementItem decrementItem) {
                currentDelta = BigDecimal.valueOf(decrementItem.getDelta()).negate();
            } else {
                throw new ConstrainViolationException(
                        "line_item", "line item not supported for change type");
            }

            this.updatePrice(type, delta, lineItem.getDiscountUnitPrice(), currentDelta);
            change.update(type, action);
        }

        if (lineItem.isTaxable()) {
            var taxLine = this.taxLines.stream()
                    .filter(tax -> Objects.equals(tax.getLineItemId(), String.valueOf(lineItem.getId())))
                    .findFirst()
                    .orElse(null);
            if (taxLine == null) {
                createNewTaxLine(lineItem, taxSetting, getEditCurrency());
            } else {
                log.info("update");
            }
        }

        this.modifiedAt = Instant.now();
    }

    private void createNewTaxLine(LineItem lineItem, TaxSetting taxSetting, Currency currency) {

    }

    private void updatePrice(OrderStagedChange.ChangeType type, BigDecimal delta, BigDecimal discountUnitPrice, BigDecimal currentDelta) {
        BigDecimal newDelta = BigDecimal.ZERO;
        if (type == OrderStagedChange.ChangeType.increment_item) {
            newDelta = delta;
        } else if (type == OrderStagedChange.ChangeType.decrement_item) {
            newDelta = delta.negate();
        }

        BigDecimal adjustDelta = newDelta.subtract(currentDelta);
        BigDecimal adjustmentPrice = adjustDelta.multiply(discountUnitPrice);

        adjustPrice(adjustmentPrice);
    }

    private void createChange(OrderStagedChange.ChangeType type, OrderStagedChange.BaseAction action) {
        var change = new OrderStagedChange(UUID.randomUUID(), type, action);
        change.setAggRoot(this);
        this.stagedChanges.add(change);
    }

    private void removeExistingLineItemChange(LineItem lineItem, TaxSetting taxSetting, OrderStagedChange change) {
        this.stagedChanges.remove(change);

        var adjustedPrice = getAdjustedPrice(lineItem, change);
        adjustPrice(adjustedPrice);

        List<AddedTaxLine> addedTaxLines = this.taxLines.stream()
                .filter(tax -> Objects.equals(tax.getLineItemId(), String.valueOf(lineItem.getId())))
                .toList();
        if (CollectionUtils.isNotEmpty(addedTaxLines)) {
            BigDecimal totalTaxPrice = addedTaxLines.stream()
                    .map(AddedTaxLine::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.taxLines.removeAll(addedTaxLines);
            this.adjustTaxPrice(totalTaxPrice, taxSetting.isTaxIncluded());
        }
    }

    private BigDecimal getAdjustedPrice(LineItem lineItem, OrderStagedChange change) {
        BigDecimal delta = BigDecimal.ZERO;
        if (change.getAction() instanceof OrderStagedChange.IncrementItem incrementItem) {
            delta = BigDecimal.valueOf(incrementItem.getDelta()).negate();
        } else if (change.getAction() instanceof OrderStagedChange.DecrementItem decrementItem) {
            delta = BigDecimal.valueOf(decrementItem.getDelta());
        }

        BigDecimal discountedUnitPrice = lineItem.getDiscountUnitPrice();
        return delta.multiply(discountedUnitPrice);
    }
}
