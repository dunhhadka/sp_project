package org.example.order.order.domain.fulfillmentorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.OrderId;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Getter
@Table(name = "fulfillment_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FulfillmentOrder extends AggregateRoot<FulfillmentOrder> {

    @Transient
    @JsonIgnore
    @Setter
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


    public void closeKeepQuantity() {
        this.status = FulfillmentOrderStatus.closed;
    }

    public int restock(long lineItemId, int remainingQuantity) {
        if (!CollectionUtils.isEmpty(lineItems)) {
            var fulfillmentLineItem = this.lineItems.stream()
                    .filter(line -> line.getLineItemId() == lineItemId && NumberUtils.isPositive(line.getInventoryItemId()))
                    .findFirst().orElse(null);
            if (fulfillmentLineItem != null) {
                var restockQuantity = Math.min(fulfillmentLineItem.getTotalQuantity(), remainingQuantity);
                fulfillmentLineItem.restock(restockQuantity);
                this.reEvaluateStatus(true);
                return restockQuantity;
            }
        }
        return 0;
    }

    private void reEvaluateStatus(boolean shouldReOpen) {
        boolean isNoneRemaining = this.lineItems.stream().allMatch(line -> line.getRemainingQuantity() <= 0);
        if (isNoneRemaining) {
            this.status = FulfillmentOrderStatus.closed;
        } else if (shouldReOpen && status != FulfillmentOrderStatus.in_progress) {
            status = FulfillmentOrderStatus.in_progress;
        }
    }

    public Pair<List<FulfillmentOrderLineItem>, FulfillmentOrder> markAsFulfilled(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        var validLineItemInputs = this.mergeLine(lineItemInputs); // merge nếu 2 line có cùng id

        validateFulfilledLineItems(validLineItemInputs);

        var remainingLineItems = this.getRemainingLineItems();
        var remainingLineMap = this.getRemainingLineItemMap();

        List<FulfillmentOrderLineItem> fulfilledLineItems = new ArrayList<>();

        if (isFulfillAllLineItems(validLineItemInputs)) {
            remainingLineItems.forEach(line -> {
                line.fulfillAll();
                fulfilledLineItems.add(line);
            });
            removeLineItemIfEmpty();
            this.changeStatus(FulfillmentOrderStatus.closed);
            return Pair.of(fulfilledLineItems, null);
        }

        if (isFulfillAndCreateNewFulfillmentOrder()) {
            return fulfillAndCreateNewFulfillmentOrder(validLineItemInputs);
        }

        lineItemInputs.forEach(line -> {
            var lineItem = remainingLineMap.get(line.getId());
            lineItem.fulfill(line.getQuantity());
            fulfilledLineItems.add(lineItem);
        });

        removeLineItemIfEmpty();

        if (getRemainingLineItemMap().isEmpty()) {
            this.changeStatus(FulfillmentOrderStatus.closed);
        }

        return Pair.of(fulfilledLineItems, null);
    }

    private Pair<List<FulfillmentOrderLineItem>, FulfillmentOrder> fulfillAndCreateNewFulfillmentOrder(List<FulfillmentOrderLineItemInput> validLineItemInputs) {
        List<FulfillmentOrderLineItem> unFulfilledLineItems = new ArrayList<>();
        List<FulfillmentOrderLineItem> fulfilledLineItems = new ArrayList<>();

        this.getRemainingLineItems().forEach(line -> {
            var existedLine = validLineItemInputs.stream().anyMatch(l -> l.getId() == line.getId());
            if (!existedLine) {
                this.lineItems.remove(line);
                unFulfilledLineItems.add(createNewLineItem(line, line.getRemainingQuantity()));
            }
        });

        var lineItemMap = this.getRemainingLineItemMap();
        validLineItemInputs.forEach(item -> {
            var lineItem = lineItemMap.get(item.getId());
            if (item.getQuantity() >= lineItem.getRemainingQuantity()) {
                lineItem.fulfillAll();
            } else {
                lineItem.fulfillAndClose(item.getQuantity());
                unFulfilledLineItems.add(createNewLineItem(lineItem, lineItem.getRemainingQuantity() - item.getQuantity()));
            }
            fulfilledLineItems.add(lineItem);
        });

        this.changeStatus(FulfillmentOrderStatus.closed);
        var unFulfillOrder = createNewFulfillOrder(unFulfilledLineItems);
        removeLineItemIfEmpty();

        return Pair.of(fulfilledLineItems, unFulfillOrder);
    }

    private FulfillmentOrder createNewFulfillOrder(List<FulfillmentOrderLineItem> lineItems) {
        if (CollectionUtils.isEmpty(lineItems)) {
            return null;
        }
        var storeId = this.getId().getStoreId();
        var orderId = new OrderId(storeId, this.orderId);
        var fulfillmentOrder = new FulfillmentOrder(idGenerator, orderId, this.assignedLocationId, this.assignedLocation,
                this.expectedDeliveryMethod, this.requireShipping, this.destination, this.fulfillOn);
        lineItems.forEach(line -> fulfillmentOrder.addLineItem(orderId, line.getLineItemId(), (long) line.getInventoryItemId(),
                line.getVariantId(), line.getVariantInfo(), line.getRemainingQuantity()));
        return fulfillmentOrder;
    }

    private FulfillmentOrderLineItem createNewLineItem(FulfillmentOrderLineItem line, Integer quantity) {
        return new FulfillmentOrderLineItem(idGenerator.generateFulfillmentOrderLineItemId(), this.orderId,
                line.getLineItemId(), (long) line.getInventoryItemId(), line.getVariantId(),
                line.getVariantInfo(), quantity);
    }

    private boolean isFulfillAndCreateNewFulfillmentOrder() {
        return FulfillmentOrderRequestStatus.rejected == this.requestStatus
                && FulfillmentOrderStatus.open == this.status;
    }

    private void changeStatus(FulfillmentOrderStatus fulfillmentOrderStatus) {
        this.status = fulfillmentOrderStatus;
    }

    private void removeLineItemIfEmpty() {
        this.lineItems.removeIf(line -> line.getRemainingQuantity() == 0 && line.getTotalQuantity() == 0);
    }

    private List<FulfillmentOrderLineItem> getRemainingLineItems() {
        return this.lineItems.stream()
                .filter(line -> line.getRemainingQuantity() > 0)
                .toList();
    }

    private List<FulfillmentOrderLineItemInput> mergeLine(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        Map<Integer, FulfillmentOrderLineItemInput> result = new HashMap<>();
        for (var line : lineItemInputs) {
            var lineId = line.getId();
            var lineItem = result.get(lineId);
            if (lineItem == null) {
                lineItem = new FulfillmentOrderLineItemInput(lineId, line.getQuantity());
                result.put(lineId, lineItem);
                continue;
            }
            lineItem.add(line.getQuantity());
        }
        return result.values().stream().toList();
    }

    private boolean isFulfillAllLineItems(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        if (CollectionUtils.isEmpty(lineItemInputs)) {
            return true;
        }

        var lineItemQuantityInputMap = lineItemInputs.stream()
                .collect(Collectors.toMap(FulfillmentOrderLineItemInput::getId, FulfillmentOrderLineItemInput::getQuantity));
        return this.getRemainingLineItemMap().entrySet().stream()
                .allMatch(entry -> {
                    var lineId = entry.getKey();
                    var lineItem = entry.getValue();
                    return Objects.equals(lineItem.getRemainingQuantity(), lineItemQuantityInputMap.get(lineId));
                });
    }

    private void validateFulfilledLineItems(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        if (FulfillmentOrderStatus.closed == this.status && FulfillmentOrderRequestStatus.submitted == this.requestStatus) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_allowed")
                    .fields(List.of("status", "request_status"))
                    .message("Cannot fulfill fulfillment_order.Stats not allowed")
                    .build());
        }

        if (CollectionUtils.isEmpty(lineItemInputs)) {
            return;
        }

        var remainingLineItemMap = this.getRemainingLineItemMap();
        if (remainingLineItemMap.isEmpty()) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_allowed")
                    .fields(List.of("fulfillment_order_line_item"))
                    .message("All line_items have been fulfilled")
                    .build());
        }

        boolean notExistLine = lineItemInputs.stream()
                .anyMatch(line -> !remainingLineItemMap.containsKey(line.getId()));
        if (notExistLine) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_exist")
                    .fields(List.of("fulfillment_order_line_item_id"))
                    .message("fulfillment_order_line_item not found")
                    .build());
        }

        boolean notFulfill = lineItemInputs.stream()
                .anyMatch(line -> {
                    var lineId = line.getId();
                    var lineITem = remainingLineItemMap.get(lineId);
                    return line.getQuantity() > lineITem.getRemainingQuantity();
                });
        if (notFulfill) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_fulfill")
                    .fields(List.of("quantity"))
                    .message("can not fulfill quantity greater than remaining quantity")
                    .build());
        }
    }

    private Map<Integer, FulfillmentOrderLineItem> getRemainingLineItemMap() {
        return this.lineItems.stream()
                .collect(Collectors.toMap(FulfillmentOrderLineItem::getId, Function.identity()));
    }

    public FulfillmentOrder move(Long newLocationId, AssignedLocation newAssignLocation) {
        if (FulfillmentOrderStatus.in_progress == this.status) {
            var storeId = this.getId().getStoreId();
            var orderId = new OrderId(storeId, this.getOrderId());
            var movedFulfillmentOrder = new FulfillmentOrder(idGenerator, orderId, newLocationId,
                    newAssignLocation, this.expectedDeliveryMethod, this.requireShipping, this.destination, this.fulfillOn);
            this.lineItems.forEach(line -> movedFulfillmentOrder.addLineItem(orderId, line.getLineItemId(), (long) line.getInventoryItemId(),
                    line.getVariantId(), line.getVariantInfo(), line.getRemainingQuantity()));
            this.closeEntry();
            return movedFulfillmentOrder;
        }

        this.changeLocationId(newLocationId, newAssignLocation);
        return this;
    }

    private void changeLocationId(Long newLocationId, AssignedLocation newAssignLocation) {
        this.assignedLocation = newAssignLocation;
        this.assignedLocationId = newLocationId;
    }

    private void closeEntry() {
        this.lineItems.forEach(FulfillmentOrderLineItem::closeEntry);
        this.changeStatus(FulfillmentOrderStatus.closed);
    }

    public void reopen() {
        this.status = FulfillmentOrderStatus.open;
        this.requestStatus = FulfillmentOrderRequestStatus.unsubmitted;
    }

    public void partialFulfillStatus() {
        this.status = FulfillmentOrderStatus.in_progress;
        this.requestStatus = FulfillmentOrderRequestStatus.unsubmitted;
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
