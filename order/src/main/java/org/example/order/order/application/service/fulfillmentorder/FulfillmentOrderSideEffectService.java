package org.example.order.order.application.service.fulfillmentorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.model.order.request.AdjustmentRequest;
import org.example.order.order.application.model.order.request.InventoryAdjustmentTransactionChangeRequest;
import org.example.order.order.application.model.order.request.InventoryTransactionLineItemRequest;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderLineItem;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.refund.event.RefundCreatedAppEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentOrderSideEffectService {

    private static final String REASON_ORDER_ITEM_RESTOCK = "order_item_restock";
    private static final String ORDER_EDIT = "order_edit";
    private static final String REFERENCE_DOCUMENT_TYPE_ORDER = "order";


    private final FulfillmentOrderRepository fulfillmentOrderRepository;

    private final ApplicationEventPublisher eventPublisher;

    private static final Comparator<FulfillmentOrder> _ffoRestockComparatorInstance = Comparator
            // sort theo status, close sẽ để xuống dưới
            .comparing(FulfillmentOrder::getStatus, (s1, s2) -> {
                if (s1 == s2) return 0;
                return s1 == FulfillmentOrder.FulfillmentOrderStatus.closed ? 1 : -1;
            })
            // sort theo assign_location_id theo thứ tự từ nhỏ đến lớn
            .thenComparingLong(FulfillmentOrder::getAssignedLocationId)
            // sort thep id, nếu status != close -> id:asc, status == close -> id:desc
            .thenComparing((o1, o2) -> {
                var compareIdResult = Long.compare(o1.getId().getId(), o2.getId().getId());
                return o1.getStatus() != FulfillmentOrder.FulfillmentOrderStatus.closed
                        ? compareIdResult
                        : -compareIdResult;
            });


    @EventListener(RefundCreatedAppEvent.class)
    public void handleRefundRestockEvent(RefundCreatedAppEvent event) {
        log.debug("handle order restocked fulfillment order: {}", event);
        if (CollectionUtils.isEmpty(event.getRestockLineItems())) {
            return;
        }

        var storeId = event.getStoreId();
        var orderId = event.getOrderId();

        var fulfillmentOrders = fulfillmentOrderRepository.findByOrderId(storeId, (int) orderId).stream()
                .filter(fo -> fo.getLineItems().stream().anyMatch(line -> NumberUtils.isPositive(line.getRemainingQuantity())))
                .sorted(_ffoRestockComparatorInstance)
                .toList();
        if (fulfillmentOrders.isEmpty()) {
            return;
        }

        List<InventoryAdjustmentItem> restockedItems = new ArrayList<>();
        List<InventoryAdjustmentItem> removedItems = new ArrayList<>();

        Map<Integer, Integer> inventoryItemMap = new HashMap<>(); // lấy inventoryItem của foLineItem, lấy inventoryItem của foLine đầu tiên
        boolean hasRestocked = event.getRestockLineItems().stream().anyMatch(RefundCreatedAppEvent.RestockLineItem::isRestock);
        if (hasRestocked) {
            inventoryItemMap = fulfillmentOrders.stream()
                    .flatMap(fo -> fo.getLineItems().stream())
                    .filter(foLine -> NumberUtils.isPositive(foLine.getInventoryItemId()))
                    .collect(Collectors.toMap(
                            FulfillmentOrderLineItem::getLineItemId,
                            FulfillmentOrderLineItem::getInventoryItemId,
                            (left, right) -> left
                    ));
        }
        // tạo event adjust inventory trước khi giảm quantity thật trong fulfillment_order
        for (var restockLineItem : event.getRestockLineItems()) {
            // ignore fulfilled và no_restock
            if (restockLineItem.isRemoval() && !restockLineItem.isRestock()) {
                continue;
            }

            // nếu như có hoàn kho -> tăng available, onHand của kho hoàn
            if (restockLineItem.isRestock()) {
                Integer inventoryItemId = inventoryItemMap.get((int) restockLineItem.lineItemId());
                if (inventoryItemId == null) continue;
                restockedItems.add(new InventoryAdjustmentItem(
                        restockLineItem.lineItemId(),
                        inventoryItemId,
                        restockLineItem.locationId(),
                        restockLineItem.quantity()
                ));
            }

            // giảm commit, onHand của kho gốc
            // giảm lần lượt cho các fulfillmentOrder chứa lineItem này theo thứ tự locationId từ bé đến lớn
            if (restockLineItem.isRemoval()) {
                int remainingQuantity = restockLineItem.quantity();
                for (var restockFulfillmentOrder : fulfillmentOrders) {
                    if (remainingQuantity < 0) {
                        break;
                    }
                    var restockFulfillmentOrderLineItem = restockFulfillmentOrder.getLineItems().stream()
                            .filter(line -> line.getLineItemId() == restockLineItem.lineItemId() && NumberUtils.isPositive(line.getInventoryItemId()))
                            .findFirst().orElse(null);
                    if (restockFulfillmentOrderLineItem == null) {
                        continue;
                    }

                    int quantity = Math.min(remainingQuantity, restockFulfillmentOrderLineItem.getTotalQuantity());
                    if (quantity > 0) {
                        remainingQuantity -= quantity;
                        removedItems.add(new InventoryAdjustmentItem(
                                restockFulfillmentOrderLineItem.getLineItemId(),
                                restockFulfillmentOrderLineItem.getInventoryItemId(),
                                restockFulfillmentOrder.getAssignedLocationId(),
                                quantity
                        ));
                    }
                }
            }
        }

        // giảm total_quantity/remaining_quantity của các item chưa fulfilled
        Map<FulfillmentOrderId, FulfillmentOrder> updateFulfillmentOrders = new HashMap<>();
        for (var restockItem : event.getRestockLineItems()) {
            var remainingQuantity = restockItem.quantity();
            for (var restockFulfillmentOrder : fulfillmentOrders) {
                if (remainingQuantity < 0) {
                    break;
                }
                var quantity = restockFulfillmentOrder.restock(restockItem.lineItemId(), remainingQuantity);
                if (quantity > 0) {
                    remainingQuantity -= quantity;
                    updateFulfillmentOrders.put(restockFulfillmentOrder.getId(), restockFulfillmentOrder);
                }
            }
        }

        // đơn hàng đã close, nếu có fulfillment_order nào có status != closed -> chuyển qua closed
        if (event.getOrder().getCancelledOn() != null) {
            for (var ffoToCancel : fulfillmentOrders) {
                if (ffoToCancel.getStatus() != FulfillmentOrder.FulfillmentOrderStatus.closed) {
                    ffoToCancel.closeKeepQuantity();
                    updateFulfillmentOrders.putIfAbsent(ffoToCancel.getId(), ffoToCancel);
                }
            }
        }
        if (!updateFulfillmentOrders.isEmpty()) {
            for (var fulfillmentOrder : updateFulfillmentOrders.values()) {
                fulfillmentOrderRepository.save(fulfillmentOrder);
            }
        }

        if (!restockedItems.isEmpty() || !removedItems.isEmpty()) {
            eventPublisher.publishEvent(new RefundAdjustInventoryAppEvent(event, restockedItems, removedItems));
        }
    }

    @TransactionalEventListener(classes = RefundAdjustInventoryAppEvent.class, phase = TransactionPhase.BEFORE_COMMIT)
    public void handleRestockInventory(RefundAdjustInventoryAppEvent event) {
        final var originalEvent = event.refundEvent();

        String adjustReason = !originalEvent.getReason().equals("edit") ? REASON_ORDER_ITEM_RESTOCK : ORDER_EDIT;
        String documentUrl = buildReferenceDocumentUrl(REFERENCE_DOCUMENT_TYPE_ORDER, originalEvent.getOrderId());

        List<AdjustmentRequest> adjustmentRequests = new ArrayList<>();

        // + available, +onHand kho hàng
        for (var restockedItem : event.restockItems()) {
            var adjustmentQuantity = BigDecimal.valueOf(restockedItem.delta());
            var availableTransactionChangeRequest = InventoryAdjustmentTransactionChangeRequest.builder()
                    .value(adjustmentQuantity)
                    .valueType("available")
                    .changeType("delta")
                    .build();
            var onHandTransactionChangeRequest = InventoryAdjustmentTransactionChangeRequest.builder()
                    .value(adjustmentQuantity)
                    .valueType("delta")
                    .changeType("onHand")
                    .build();
            var restockTransactionLineItemRequest = InventoryTransactionLineItemRequest.builder()
                    .inventoryItemId(restockedItem.inventoryItemId())
                    .changes(List.of(availableTransactionChangeRequest, onHandTransactionChangeRequest))
                    .build();
            var adjustmentRequest = AdjustmentRequest.builder()
                    .locationId(restockedItem.locationId())
                    .reason(adjustReason)
                    .referenceDocumentUrl(documentUrl)
                    .lineItems(List.of(restockTransactionLineItemRequest))
                    .build();
            adjustmentRequests.add(adjustmentRequest);
        }

        // -commit, -onHand kho gốc
        for (var removedItem : event.restockItems) {
            if (removedItem.locationId() == 0) continue;
            var adjustmentQuantity = BigDecimal.valueOf(removedItem.delta()).negate();
            var committedTransactionChangeRequest = InventoryAdjustmentTransactionChangeRequest.builder()
                    .value(adjustmentQuantity)
                    .build();
            var onHandTransactionChangeRequest = InventoryAdjustmentTransactionChangeRequest.builder()
                    .value(adjustmentQuantity)
                    .build();
            var transactionLineRequest = InventoryTransactionLineItemRequest.builder()
                    .inventoryItemId(removedItem.inventoryItemId())
                    .changes(List.of(committedTransactionChangeRequest, onHandTransactionChangeRequest))
                    .build();

            var adjustmentRequest = AdjustmentRequest.builder()
                    .locationId(removedItem.locationId())
                    .reason(adjustReason)
                    .referenceDocumentUrl(documentUrl)
                    .lineItems(List.of(transactionLineRequest))
                    .build();
            adjustmentRequests.add(adjustmentRequest);
        }

        if (adjustmentRequests.isEmpty()) return;

        // get author
    }

    private String buildReferenceDocumentUrl(String referenceType, long id) {
        return "/admin/" + convertReferentTypeToResourcePath(referenceType) + "/" + id;
    }

    private String convertReferentTypeToResourcePath(String referenceType) {
        return switch (referenceType) {
            case REFERENCE_DOCUMENT_TYPE_ORDER -> "orders";
            default -> throw new IllegalArgumentException("Invalid reference type: " + referenceType);
        };
    }

    public record RefundAdjustInventoryAppEvent(
            RefundCreatedAppEvent refundEvent,
            List<InventoryAdjustmentItem> restockItems,
            List<InventoryAdjustmentItem> removeItems
    ) {
    }

    public record InventoryAdjustmentItem(
            long lineItemId,
            int inventoryItemId,
            long locationId,
            int delta
    ) {
    }


    @TransactionalEventListener(classes = {MovedFulfillmentOrderEvent.class}, phase = TransactionPhase.BEFORE_COMMIT)
    public void handleFulfillmentOrderMoved(MovedFulfillmentOrderEvent event) {
        log.debug("handle fulfillment order movoed: {}", event);

        var originalFulfillmentOrderId = event.originalFulfillmentOrderId();
        var originalFulfillmentOrder = fulfillmentOrderRepository.findById(originalFulfillmentOrderId)
                .orElseThrow(NotFoundException::new);
        var inventoryBehavior = originalFulfillmentOrder.getInventoryBehaviour();
        if (FulfillmentOrder.InventoryBehaviour.bypass == inventoryBehavior) {
            return;
        } else {
            inventoryBehavior = FulfillmentOrder.InventoryBehaviour.decrement_obeying_policy;
        }

        var movedFulfillmentOrderId = event.movedFulfillmentOrderId();
        var movedFulfillmentOrder = fulfillmentOrderRepository.findById(movedFulfillmentOrderId)
                .orElseThrow(NotFoundException::new);

        var originalLocationId = event.originalLocationId();
        var movedLocationId = event.newLocationId();

        InventoryRequest inventoryRequest = null;

        var originalLocationLineItemRequest = movedFulfillmentOrder.getLineItems().stream()
                .filter(lineItem -> NumberUtils.isPositive(lineItem.getInventoryItemId()))
                .map(lineItem -> {
                    var movedQuantity = BigDecimal.valueOf(lineItem.getRemainingQuantity());
                    var availableAdjustmentRequest = InventoryAdjustmentTransactionChangeRequest.builder()
                            .value(movedQuantity)
                            .valueType("delta")
                            .changeType("available")
                            .build();
                    var committedAdjustmentRequest = InventoryAdjustmentTransactionChangeRequest.builder()
                            .value(movedQuantity.negate())
                            .valueType("delta")
                            .changeType("committed")
                            .build();
                    List<InventoryAdjustmentTransactionChangeRequest> changes = List.of(availableAdjustmentRequest, committedAdjustmentRequest);
                    return InventoryTransactionLineItemRequest.builder()
                            .inventoryItemId(lineItem.getInventoryItemId())
                            .changes(changes)
                            .build();
                })
                .toList();

    }
}
