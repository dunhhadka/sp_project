package org.example.order.order.domain.orderedit.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.service.orderedit.EditContextService;
import org.example.order.order.application.utils.TaxSetting;
import org.example.order.order.application.utils.TaxSettingValue;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.domain.order.model.LineItem;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Slf4j
@DynamicUpdate
@NoArgsConstructor
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

    public OrderEdit(
            Currency currency,
            int orderId,
            BigDecimal subtotalLineItemQuantity,
            BigDecimal subtotalPrice,
            BigDecimal cartDiscountAmount,
            BigDecimal totalPrice,
            BigDecimal totalOutstanding
    ) {
        this.currency = currency;
        this.orderId = orderId;
        this.subtotalLineItemQuantity = subtotalLineItemQuantity;
        this.subtotalPrice = subtotalPrice;
        this.cartDiscountAmount = cartDiscountAmount;
        this.totalPrice = totalPrice;
        this.totalOutStanding = totalOutstanding;

        this.committed = false;
        this.orderVersion = 1;

        this.createdAt = Instant.now();
    }

    public void addLineItem(AddedLineItem lineItem, EditContextService.NeedTax context) {
        lineItem.setAggRoot(this);
        this.lineItems.add(lineItem);

        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity.add(lineItem.getEditableQuantity());
        this.adjustPrice(lineItem.getEditableSubtotal());

        if (!context.isTaxExempt()) {
            boolean taxIncluded = context.taxIncluded();
            TaxSettingValue taxValue = context.getTax(lineItem.getProductId());

            var taxLine = createTaxLine(lineItem, taxIncluded, taxValue);
            this.adjustTaxPrice(taxLine.getPrice(), taxIncluded);
        }

        OrderStagedChange.ChangeType type;
        OrderStagedChange.BaseAction action;
        if (lineItem.getVariantId() != null) {
            type = OrderStagedChange.ChangeType.add_variant;
            action = new OrderStagedChange.AddVariant(
                    lineItem.getVariantId(),
                    lineItem.getId(),
                    lineItem.getEditableQuantity(),
                    lineItem.getLocationId()
            );
        } else {
            type = OrderStagedChange.ChangeType.add_custom_item;
            action = new OrderStagedChange.AddCustomItem(
                    lineItem.getId(),
                    lineItem.getTitle(),
                    lineItem.getOriginalUnitPrice(),
                    lineItem.getEditableQuantity(),
                    lineItem.isTaxable(),
                    lineItem.isRequireShipping(),
                    lineItem.getLocationId()
            );
        }

        var change = new OrderStagedChange(
                UUID.randomUUID(),
                type, action
        );
        change.setAggRoot(this);
        this.stagedChanges.add(change);
    }

    private void adjustTaxPrice(BigDecimal price, boolean taxIncluded) {
        if (!taxIncluded) {
            this.totalPrice = this.totalPrice.add(price);
            this.totalOutStanding = this.totalOutStanding.add(price);
        }
    }

    private AddedTaxLine createTaxLine(AddedLineItem lineItem, boolean taxIncluded, TaxSettingValue taxValue) {
        assert taxValue != null;

        AddedTaxLine newTaxLine = new AddedTaxLine(
                UUID.randomUUID(),
                taxValue.getTitle(),
                taxValue.getRate(),
                lineItem,
                currency,
                taxIncluded
        );

        newTaxLine.setAggRoot(this);
        this.taxLines.add(newTaxLine);

        return newTaxLine;
    }

    private void adjustPrice(BigDecimal adjustment) {
        this.subtotalPrice = this.subtotalPrice.add(adjustment);
        this.totalPrice = this.totalPrice.add(adjustment);
        this.totalOutStanding = this.totalOutStanding.add(adjustment);
    }

    public void removeItem(UUID lineItemId, TaxSetting taxSetting) {
        AddedLineItem lineItem = this.lineItems.stream()
                .filter(line -> Objects.equals(line.getId(), lineItemId))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        this.lineItems.remove(lineItem);

        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity.subtract(lineItem.getEditableQuantity());
        this.adjustPrice(lineItem.getEditableSubtotal().negate());

        this.removeDiscounts(lineItemId);

        for (var taxLine : this.taxLines) {
            if (taxLine.getLineItemId().equals(lineItemId.toString())) {
                var amount = taxLine.getPrice();
                this.adjustTaxPrice(amount.negate(), taxSetting.isTaxIncluded());
                break;
            }
        }

        this.taxLines.removeIf(taxLine -> taxLine.getLineItemId().equals(lineItemId.toString()));

        this.stagedChanges.removeIf((change) -> {
            if (change.getType() == OrderStagedChange.ChangeType.add_custom_item) {
                OrderStagedChange.AddCustomItem addCustomItem = (OrderStagedChange.AddCustomItem) change.getAction();
                return addCustomItem.getLineItemId().equals(lineItemId);
            }
            if (change.getType() == OrderStagedChange.ChangeType.add_variant) {
                OrderStagedChange.AddVariant addVariant = (OrderStagedChange.AddVariant) change.getAction();
                return addVariant.getLineItemId().equals(lineItemId);
            }
            return false;
        });
    }

    private BigDecimal removeDiscounts(UUID lineItemId) {
        AddedDiscountAllocation allocation = this.discountAllocations.stream()
                .filter(discount -> discount.getLineItemId().equals(lineItemId))
                .findFirst()
                .orElse(null);
        if (allocation == null)
            return BigDecimal.ZERO;

        this.discountAllocations.remove(allocation);
        this.discountApplications.removeIf(discount -> discount.getId().toString().equals(allocation.getApplicationId()));

        this.stagedChanges.removeIf(change -> {
            if (change.getType() == OrderStagedChange.ChangeType.add_item_discount) {
                OrderStagedChange.AddItemDiscount addItemDiscount = (OrderStagedChange.AddItemDiscount) change.getAction();
                return addItemDiscount.getLineItemId().equals(lineItemId);
            }
            return false;
        });

        return allocation.getAllocatedAmount();
    }

    public void updateAddedLineItemQuantity(UUID lineItemId, TaxSetting taxSetting, int quantity) {
        AddedLineItem lineItem = this.lineItems.stream()
                .filter(line -> line.getId().equals(lineItemId))
                .findFirst()
                .orElseThrow(NotFoundException::new);

        // update quantity
        BigDecimal requestedQuantity = BigDecimal.valueOf(quantity);
        if (lineItem.getEditableQuantity().equals(requestedQuantity)) {
            log.debug("Skipping quantity update for AddedLineItem");
            return;
        }

        subtotalLineItemQuantity = subtotalLineItemQuantity
                .subtract(lineItem.getEditableQuantity())
                .add(requestedQuantity);

        lineItem.updateQuantity(requestedQuantity);

        // update discount
        this.discountAllocations.stream()
                .filter(allocation -> allocation.getLineItemId().equals(lineItemId))
                .findFirst()
                .ifPresent(allocation -> {
                    var allocatedDiscountAmount = lineItem.getTotalDiscount();
                    var adjustAmount = allocation.updateAmount(allocatedDiscountAmount);
                    this.adjustPrice(adjustAmount.negate());
                });

        // update tax
        this.recalculateLineItemTax(lineItem, taxSetting);

        // update quantity of staged change
        for (var change : this.stagedChanges) {
            if (change.getType() == OrderStagedChange.ChangeType.add_custom_item) {
                var addCustomItem = (OrderStagedChange.AddCustomItem) change.getAction();
                if (addCustomItem.getLineItemId().equals(lineItemId)) {
                    addCustomItem = addCustomItem.toBuilder()
                            .quantity(requestedQuantity)
                            .build();
                    change.update(change.getType(), addCustomItem);
                    break;
                }
            }
            if (change.getType() == OrderStagedChange.ChangeType.add_variant) {
                var addVariant = (OrderStagedChange.AddVariant) change.getAction();
                if (addVariant.getLineItemId().equals(lineItemId)) {
                    addVariant = addVariant.toBuilder()
                            .quantity(requestedQuantity)
                            .build();
                    change.update(change.getType(), addVariant);
                    break;
                }
            }
        }
    }

    private void recalculateLineItemTax(AddedLineItem lineItem, TaxSetting taxSetting) {
        if (taxSetting == null) {
            if (log.isDebugEnabled()) {
                log.debug("Shipping tax recalculation for line item");
            }
            return;
        }

        this.taxLines.stream()
                .filter(taxLine -> taxLine.getLineItemId().equals(lineItem.getId().toString()))
                .forEach(tax -> {
                    var originalTaxAmount = tax.getPrice();
                    var newTaxAmount = tax.updateQuantity(lineItem, currency, taxSetting.isTaxIncluded());
                    var adjustmentAmount = newTaxAmount.subtract(originalTaxAmount);
                    this.adjustTaxPrice(adjustmentAmount, taxSetting.isTaxIncluded());
                });
    }

    public void recordQuantityAdjustment(LineItem lineItem, int newQuantity, boolean restock, EditContextService.ExistingItemContext existingItemContext) {
        int currentQuantity = lineItem.getFulfillableQuantity();
        int delta = newQuantity - currentQuantity;

        Optional<OrderStagedChange> quantityChange = this.stagedChanges.stream()
                .filter(change -> {
                    if (change.getAction() instanceof OrderStagedChange.QuantityAdjustmentAction adjustmentAction) {
                        return adjustmentAction.getLineItemId() == lineItem.getId();
                    }
                    return false;
                }).findFirst();

        if (delta == 0) {
            if (quantityChange.isEmpty()) {
                log.debug("No pending OrderStagedChange for LineItem");
                return;
            }
            removeExistingChange(lineItem, quantityChange.get());
            this.modifiedAt = Instant.now();
            return;
        }

        OrderStagedChange.ChangeType type = delta > 0
                ? OrderStagedChange.ChangeType.increment_item
                : OrderStagedChange.ChangeType.decrement_item;
        OrderStagedChange.BaseAction action = delta > 0
                ? new OrderStagedChange.IncrementItem(lineItem.getId(), delta)
                : new OrderStagedChange.DecrementItem(lineItem.getId(), -delta, restock);
        if (quantityChange.isPresent()) {
            OrderStagedChange osc = quantityChange.get();
            updateTotal(delta - getDelta(osc), lineItem.getDiscountUnitPrice());
            osc.update(type, action);
        } else {
            var change = new OrderStagedChange(UUID.randomUUID(), type, action);
            change.setAggRoot(this);
            updateTotal(delta, lineItem.getDiscountUnitPrice());
            stagedChanges.add(change);
        }

        if (delta > 0 && lineItem.isTaxable()) {
            EditContextService.NeededTax taxContext = (EditContextService.NeededTax) existingItemContext;
            var lineItemIdString = String.valueOf(lineItem.getId());
            this.taxLines.removeIf(tax -> tax.getLineItemId().equals(lineItemIdString));

            Integer productId = lineItem.getVariantInfo().getProductId();
            TaxSettingValue value = taxContext.taxSetting().getTaxes().stream()
                    .filter(tax -> Objects.equals(tax.getProductId(), productId))
                    .findFirst()
                    .orElse(null);
            if (value != null) {
                boolean taxIncluded = taxContext.taxSetting().isTaxIncluded();
                var taxLine = createTaxLine(lineItem, taxIncluded, value, newQuantity);
                this.adjustTaxPrice(taxLine.getPrice(), taxIncluded);
            }
        }

        this.modifiedAt = Instant.now();
    }

    private AddedTaxLine createTaxLine(LineItem lineItem, boolean taxIncluded, TaxSettingValue value, int newQuantity) {
        return null;
    }

    private int getDelta(OrderStagedChange orderStagedChange) {
        int oldDelta;
        if (orderStagedChange.getAction() instanceof OrderStagedChange.IncrementItem ii) {
            oldDelta = ii.getDelta();
        } else if (orderStagedChange.getAction() instanceof OrderStagedChange.DecrementItem di) {
            oldDelta = -di.getDelta();
        } else {
            throw new IllegalArgumentException();
        }
        return oldDelta;
    }

    private void removeExistingChange(LineItem lineItem, OrderStagedChange orderStagedChange) {
        this.stagedChanges.remove(orderStagedChange);

        int oldDelta = getDelta(orderStagedChange);

        BigDecimal oldDeltaDecimal = BigDecimal.valueOf(oldDelta);
        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity
                .subtract(oldDeltaDecimal);
        BigDecimal discountedUnitPrice = lineItem.getDiscountUnitPrice();
        this.adjustPrice(discountedUnitPrice.multiply(oldDeltaDecimal).negate());

        // update price
        this.taxLines.removeIf(taxLine -> taxLine.getLineItemId().equals(String.valueOf(lineItem.getId())));
    }

    private void updateTotal(int delta, BigDecimal discountedUnitPrice) {
        BigDecimal decimal = BigDecimal.valueOf(delta);
        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity.add(decimal);
        this.adjustPrice(decimal.multiply(discountedUnitPrice));
    }

    public void removeDiscount(AddedLineItem lineItem) {
        BigDecimal removedAmount = removeDiscounts(lineItem.getId());
        if (removedAmount.signum() == 0)
            return;

        lineItem.removeDiscount();

        this.modifiedAt = Instant.now();
    }

    public void applyDiscount(AddedLineItem lineItem, DiscountRequest discountRequest) {
        BigDecimal discountValue = discountRequest.amount;
        BigDecimal amount = getDiscountAmount(discountRequest, lineItem);
        lineItem.applyDiscount(amount);

        var allocateAmount = amount.multiply(lineItem.getEditableQuantity());

        var existedAllocation = this.discountAllocations.stream()
                .filter(line -> line.getLineItemId().equals(lineItem.getId()))
                .findFirst();
        BigDecimal adjustmentAmount = existedAllocation
                .map(allocation -> updateDiscount(lineItem, allocation, allocateAmount, discountRequest))
                .orElseGet(() -> insertNewDiscount(lineItem.getId(), discountRequest));
    }

    private BigDecimal updateDiscount(AddedLineItem lineItem, AddedDiscountAllocation allocation, BigDecimal allocateAmount, DiscountRequest discountRequest) {
        AddedDiscountApplication application = this.discountApplications.stream()
                .filter(d -> d.getId().toString().equals(allocation.getApplicationId()))
                .findFirst()
                .orElseThrow();

        String description = discountRequest.description;
        application.update(description, allocateAmount, discountRequest.type);

        BigDecimal originalAmount = allocation.getAllocatedAmount();
        allocation.updateAmount(allocateAmount);

        OrderStagedChange change = this.stagedChanges.stream()
                .filter(c -> {
                    if (c.getAction() instanceof OrderStagedChange.AddItemDiscount addItemDiscount) {
                        return addItemDiscount.getLineItemId().equals(lineItem.getId());
                    }
                    return false;
                })
                .findFirst()
                .orElseThrow();
        var event = (OrderStagedChange.AddItemDiscount) change.getAction();
        change.updateEvent(event.toBuilder()
                .description(description)
                .value(allocateAmount)
                .build());

        return allocateAmount.subtract(originalAmount);
    }

    private BigDecimal insertNewDiscount(UUID id, DiscountRequest request) {
        String description = request.description;
        BigDecimal amount = request.amount;

        var application = new AddedDiscountApplication();
        application.setAggRoot(this);
        this.discountApplications.add(application);

        var allocation = new AddedDiscountAllocation();
        allocation.setAggRoot(this);
        this.discountAllocations.add(allocation);
        return null;
    }

    private BigDecimal getDiscountAmount(DiscountRequest discountRequest, AddedLineItem lineItem) {
        return switch (discountRequest.type) {
            case fixed_amount -> discountRequest.amount.min(lineItem.getEditableSubtotal());
            case percentage -> {
                var percentValue = discountRequest.amount;
                yield percentValue.multiply(lineItem.getOriginalUnitPrice())
                        .movePointLeft(2)
                        .setScale(currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            }
        };
    }

    public record DiscountRequest(DiscountApplication.ValueType type, BigDecimal amount, String description) {
    }
}
