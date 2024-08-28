package org.example.order.order.domain.fulfillmentorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.OrderId;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Getter
@Table(name = "fulfillment_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FulfillmentOrder extends AggregateRoot<FulfillmentOrder> {

    @Transient
    @JsonIgnore
    private FulfillmentOrderIdGenerator idGenerator;

    @EmbeddedId
    @JsonUnwrapped
    @AttributeOverride(name = "storeId", column = @Column(name = "storeId"))
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    private FulfillmentOrderId id;

    @Min(1)
    private int orderId;

    @NotNull
    private Long assignedLocationId;

    @Enumerated(value = EnumType.STRING)
    private ExpectedDeliveryMethod expectedDeliveryMethod;

    private Boolean requireShipping;

    @Enumerated(value = EnumType.STRING)
    private InventoryBehaviour inventoryBehaviour;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private FulfillmentOrderStatus status;

    @Enumerated(value = EnumType.STRING)
    private FulfillmentOrderRequestStatus requestStatus;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "assignedLocationName")),
            @AttributeOverride(name = "phone", column = @Column(name = "assignedLocationPhone")),
            @AttributeOverride(name = "email", column = @Column(name = "assignedLocationEmail")),
            @AttributeOverride(name = "address", column = @Column(name = "assignedLocationAddress"))
    })
    private AssignedLocation assignedLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName", column = @Column(name = "destinationFirstName")),
            @AttributeOverride(name = "lastName", column = @Column(name = "destinationLastName")),
            @AttributeOverride(name = "phone", column = @Column(name = "destinationPhone")),
            @AttributeOverride(name = "email", column = @Column(name = "destinationEmail")),
            @AttributeOverride(name = "address", column = @Column(name = "destinationAddress"))
    })
    private Destination destination;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid FulfillmentOrderLineItem> lineItems = new ArrayList<>();

    @Column(name = "DATETIME2")
    private Instant fulfillOn;

    private boolean hasThirdParty;

    public FulfillmentOrder(
            FulfillmentOrderIdGenerator idGenerator,
            OrderId orderId,
            Long assignedLocationId,
            AssignedLocation assignedLocation,
            ExpectedDeliveryMethod expectedDeliveryMethod,
            boolean requireShipping,
            Destination destination,
            Instant fulfillOn
    ) {
        this.idGenerator = idGenerator;
        this.id = new FulfillmentOrderId(orderId.getStoreId(), idGenerator.generateFulfillmentOrderId());
        this.orderId = orderId.getId();
        this.assignedLocationId = assignedLocationId;
        this.assignedLocation = assignedLocation;
        this.expectedDeliveryMethod = expectedDeliveryMethod;
        this.requireShipping = requireShipping;
        this.destination = destination;
        this.fulfillOn = fulfillOn;
        this.status = FulfillmentOrderStatus.open;
        this.inventoryBehaviour = InventoryBehaviour.bypass;
        hasThirdParty = false;
    }

    public void addLineItem(
            OrderId orderId,
            int lineItemId,
            Long inventoryItemId,
            Integer variantId,
            ProductVariantInfo productVariantInfo,
            int quantity
    ) {
        FulfillmentOrderLineItem lineItem = new FulfillmentOrderLineItem(
                idGenerator.generateFulfillmentOrderLineItemId(),
                orderId.getId(),
                lineItemId,
                inventoryItemId,
                variantId,
                productVariantInfo,
                quantity
        );
        lineItem.setAggRoot(this);
        if (lineItems == null) lineItems = new ArrayList<>();
        lineItems.add(lineItem);
    }

    public List<FulfillmentOrderLineItem> markFulfilled(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        validateFulfillmentLineItem(lineItemInputs);

        var remainingLineItems = this.getRemainingQuantity();
        var remainingLineItemMap = this.getRemainingQuantityMap();

        var fulfilledLineItems = new ArrayList<FulfillmentOrderLineItem>();
        if (isFulfilledAllLineItem(lineItemInputs)) {
            remainingLineItems.forEach(lineItem -> {
                lineItem.fulfill();
                fulfilledLineItems.add(lineItem);
            });
            removeLineItemIfEmpty();
            this.status = FulfillmentOrderStatus.closed;
            return fulfilledLineItems;
        }

        if (this.hasThirdParty) {
            return fulfilledAndCreateNewFulfillmentOrder(lineItemInputs);
        }
        return fulfilledLineItems;
    }

    private List<FulfillmentOrderLineItem> fulfilledAndCreateNewFulfillmentOrder(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        List<FulfillmentOrderLineItem> unFulfilledLineItems = new ArrayList<>();
        List<FulfillmentOrderLineItem> fulfilledLineItems = new ArrayList<>();

        this.getRemainingQuantity().stream()
                .filter(f -> lineItemInputs.stream().noneMatch(r -> Objects.equals(r.getId(), f.getId())))
                .forEach(lineItem -> {
                    this.lineItems.remove(lineItem);
                    unFulfilledLineItems.add(createNewFulfillmentOrderLineItem(lineItem, lineItem.getRemainingQuantity()));
                });

        lineItemInputs.forEach(itemInput -> {
            var lineItem = this.getRemainingQuantityMap().get(itemInput.getId());
            var requestedQuantity = itemInput.getQuantity();

            if (requestedQuantity >= lineItem.getRemainingQuantity()) {
                lineItem.fulfill();
                fulfilledLineItems.add(lineItem);
            } else {
                var unfulfilledLineItem = createNewFulfillmentOrderLineItem(lineItem, lineItem.getRemainingQuantity() - requestedQuantity);
                lineItem.fulfillAndClose(requestedQuantity);
                unFulfilledLineItems.add(unfulfilledLineItem);
            }
        });

        var unfulfilledFulfillmentOrder = createNewFulfillmentOrder();
        removeLineItemIfEmpty();
        return fulfilledLineItems;
    }

    private FulfillmentOrder createNewFulfillmentOrder() {
        return null;
    }

    private FulfillmentOrderLineItem createNewFulfillmentOrderLineItem(FulfillmentOrderLineItem lineItem, int quantity) {
        FulfillmentOrderLineItem fulfillmentOrderLineItem = new FulfillmentOrderLineItem(
                idGenerator.generateFulfillmentOrderLineItemId(),
                lineItem.getOrderId(),
                lineItem.getLineItemId(),
                (long) lineItem.getInventoryItemId(),
                lineItem.getVariantId(),
                lineItem.getVariantInfo(),
                quantity
        );
        fulfillmentOrderLineItem.setAggRoot(this);

        return fulfillmentOrderLineItem;
    }


    private void removeLineItemIfEmpty() {
        this.lineItems.removeIf(l -> l.getRemainingQuantity() == 0);
    }

    private boolean isFulfilledAllLineItem(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        if (CollectionUtils.isEmpty(lineItemInputs)) return false;

        var lineItemInputMap = lineItemInputs.stream()
                .collect(Collectors.toMap(FulfillmentOrderLineItemInput::getId, FulfillmentOrderLineItemInput::getQuantity));
        return this.getRemainingQuantity().stream()
                .allMatch(line -> {
                    var quantity = lineItemInputMap.get(line.getId());
                    return quantity != null && quantity >= line.getRemainingQuantity();
                });
    }

    private void validateFulfillmentLineItem(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        if (CollectionUtils.isEmpty(lineItemInputs)) return;

        if (FulfillmentOrderStatus.open == this.status && this.requestStatus == FulfillmentOrderRequestStatus.submitted) {
            throw new ConstrainViolationException(
                    "status",
                    "cannot fulfill fulfillment order, status is not allowed"
            );
        }

        var remainingLineItems = this.getRemainingQuantity();
        var remainingLineItemMap = this.getRemainingQuantityMap();

        if (remainingLineItems.isEmpty()) {
            throw new ConstrainViolationException(
                    "not_allowed",
                    "all line_items have been fulfilled"
            );
        }

        lineItemInputs.forEach(line -> {
            var lineItem = remainingLineItemMap.get(line.getId());
            if (lineItem == null) {
                throw new ConstrainViolationException(
                        "line_item",
                        "The fulfillment order line items does not exist"
                );
            }
        });
    }

    private Map<Integer, FulfillmentOrderLineItem> getRemainingQuantityMap() {
        return this.lineItems.stream()
                .filter(l -> l.getRemainingQuantity() != null)
                .collect(Collectors.toMap(FulfillmentOrderLineItem::getId, Function.identity()));
    }

    private List<FulfillmentOrderLineItem> getRemainingQuantity() {
        return this.lineItems.stream()
                .filter(l -> NumberUtils.isPositive(l.getRemainingQuantity()))
                .toList();
    }

    public enum ExpectedDeliveryMethod {
        none, // không vận chuyển
        retail, // bán tại cửa hàng bán lẻ (offline)
        pick_up, // nhận tại cửa hàng (mua online, nhận offline)
        external_service, // đối tác vận chuyển tích hợp
        external_shipper, // đối tác vận chuyển ngoài
        shipping,
    }

    public enum InventoryBehaviour {
        bypass, decrement_ignoring_policy, decrement_obeying_policy, decrement_obeying_policy_in_specify_location
    }

    public enum FulfillmentOrderStatus {
        open, in_progress, cancelled, scheduled, on_hold, incomplete, closed
    }

    public enum FulfillmentOrderRequestStatus {
        unsubmitted, submitted, accepted, rejected, cancellation_requested, cancellation_accepted, cancellation_rejected, closed
    }
}
