package org.example.order.order.domain.fulfillmentorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.*;
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
@Builder
@AllArgsConstructor
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


    public void reopen() {
        this.status = FulfillmentOrderStatus.open;
        this.requestStatus = FulfillmentOrderRequestStatus.unsubmitted;
    }

    public void partialFulfillStatus() {
        this.status = FulfillmentOrderStatus.in_progress;
        this.requestStatus = FulfillmentOrderRequestStatus.unsubmitted;
    }

    public FulfilledResult markAsFulfilled(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        validateLineInputs(lineItemInputs);
        var validLineItemInputs = getValidInputs(lineItemInputs);
        if (CollectionUtils.isEmpty(validLineItemInputs)) {
            return new FulfilledResult(this, List.of());
        }

        var remainingLineItems = getRemainingLineItems();
        List<FulfillmentOrderLineItem> fulfillmentOrderLineItems = new ArrayList<>();

        if (isFulfillAllLineItem(validLineItemInputs)) {
            remainingLineItems.forEach(lineItem -> {
                lineItem.fulfillAll();
                fulfillmentOrderLineItems.add(lineItem);
            });
            this.closeKeepQuantity();
            return new FulfilledResult(this, fulfillmentOrderLineItems);
        }

        if (isFulfillAndCreateNewFulfillmentOrder()) {
            return fulfillAndCreateNewFulfillmentOrder(validLineItemInputs);
        }
        return null;
    }

    private FulfilledResult fulfillAndCreateNewFulfillmentOrder(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        List<FulfillmentOrderLineItem> unFulfilledLineItems = new ArrayList<>();
        List<FulfillmentOrderLineItem> fulfilledLineItems = new ArrayList<>();

        this.getRemainingLineItems().stream()
                .filter(f -> lineItemInputs.stream().noneMatch(line -> Objects.equals(line.getId(), f.getId())))
                .forEach(line -> {
                    this.lineItems.remove(line);
                    unFulfilledLineItems.add(new FulfillmentOrderLineItem());
                });

        var remainingLineItemMap = getRemainingLineItemMap();
        lineItemInputs.forEach(itemInput -> {
            var lineItem = remainingLineItemMap.get(itemInput.getId());
            int requestedQuantity = itemInput.getQuantity();

            if (requestedQuantity >= lineItem.getRemainingQuantity()) {
                lineItem.fulfillAll();
            } else {
                var unFulfilledLineItem = new FulfillmentOrderLineItem();
                lineItem.fulfillAndClose(requestedQuantity);
                unFulfilledLineItems.add(unFulfilledLineItem);
            }
        });

        return null;
    }

    private boolean isFulfillAndCreateNewFulfillmentOrder() {
        return true;
    }

    private List<FulfillmentOrderLineItemInput> getValidInputs(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        Map<Integer, FulfillmentOrderLineItemInput> results = new HashMap<>();
        for (var input : lineItemInputs) {
            var lineItem = results.get(input.getId());
            if (lineItem == null) {
                lineItem = new FulfillmentOrderLineItemInput(input.getId(), input.getQuantity());
                results.put(lineItem.getId(), lineItem);
                continue;
            }
            lineItem.add(input.getQuantity());
            results.put(lineItem.getId(), lineItem);
        }
        return results.values().stream().toList();
    }

    private boolean isFulfillAllLineItem(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        var remainingLineItemMap = getRemainingLineItemMap();
        return lineItemInputs.stream()
                .allMatch(input -> {
                    var lineItem = remainingLineItemMap.get(input.getId());
                    return lineItem != null && Objects.equals(lineItem.getRemainingQuantity(), input.getQuantity());
                });
    }

    private void validateLineInputs(List<FulfillmentOrderLineItemInput> lineItemInputs) {
        if (this.status != FulfillmentOrderStatus.open) {
            throw new ConstrainViolationException(UserError.builder()
                    .message("")
                    .fields(List.of("status"))
                    .build());
        }
        if (CollectionUtils.isEmpty(lineItemInputs)) {
            return;
        }
        // phải get remaining tại vì tất cả tính toán sẽ dựa trên remaining này
        var remainingLineItems = getRemainingLineItems();
        if (CollectionUtils.isEmpty(remainingLineItems)) {
            throw new ConstrainViolationException(UserError.builder()
                    .message("fulfillment order have fulfilled")
                    .fields(List.of("fulfillment_line_items"))
                    .build());
        }

        var remainingLineItemMap = getRemainingLineItemMap();
        Map<Integer, Integer> remainingInputs = new HashMap<>();
        lineItemInputs.forEach(line -> {
            var lineItem = remainingLineItemMap.get(line.getId());
            if (lineItem == null) {
                throw new ConstrainViolationException(UserError.builder()
                        .message("line_item not found")
                        .fields(List.of("id"))
                        .build());
            }
            int quantity = remainingInputs.getOrDefault(line.getId(), 0) + line.getQuantity();
            if (quantity > lineItem.getRemainingQuantity()) {
                throw new ConstrainViolationException(UserError.builder()
                        .message("total quantity must be less than or equal to remaining of line")
                        .fields(List.of("quantity"))
                        .build());
            }
            remainingInputs.put(line.getId(), quantity);
        });
    }

    private Map<Integer, FulfillmentOrderLineItem> getRemainingLineItemMap() {
        return Optional.ofNullable(this.lineItems)
                .map(lines ->
                        lines.stream()
                                .filter(line -> NumberUtils.isPositive(line.getRemainingQuantity()))
                                .collect(Collectors.toMap(
                                        FulfillmentOrderLineItem::getId,
                                        Function.identity())))
                .orElse(Map.of());
    }

    private List<FulfillmentOrderLineItem> getRemainingLineItems() {
        return Optional.ofNullable(this.lineItems)
                .map(lines ->
                        lines.stream()
                                .filter(line -> NumberUtils.isPositive(line.getRemainingQuantity()))
                                .toList())
                .orElse(List.of());
    }

    public FulfillmentOrder move(long newLocationId, AssignedLocation newLocation) {
        FulfillmentOrder movedFulfillmentOrder = this;

        if (FulfillmentOrderStatus.in_progress == this.status) {
            var movedFulfillmentOrderId = new FulfillmentOrderId(this.id.getStoreId(), idGenerator.generateFulfillmentOrderId());
            movedFulfillmentOrder = new FulfillmentOrder();
            for (var lineItem : this.lineItems) {
                if (lineItem.getRemainingQuantity() > 0) {
                    movedFulfillmentOrder.addLineItem(new OrderId(this.id.getStoreId(), this.orderId),
                            this.idGenerator.generateFulfillmentOrderLineItemId(),
                            (long) lineItem.getInventoryItemId(),
                            lineItem.getVariantId(),
                            lineItem.getVariantInfo(),
                            lineItem.getRemainingQuantity()
                    );
                }
            }
            this.closeEntry();
        } else {
            this.changeLocation(newLocationId, newLocation);
        }

        return movedFulfillmentOrder;
    }

    private void changeLocation(long newLocationId, AssignedLocation newLocation) {
        this.assignedLocation = newLocation;
        this.assignedLocationId = newLocationId;
    }

    private void closeEntry() {
        this.status = FulfillmentOrderStatus.closed;
    }

    public record FulfilledResult(FulfillmentOrder fulfillmentOrder, List<FulfillmentOrderLineItem> lineItems) {
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
