package org.example.order.order.application.service.fulfillmentorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.model.order.request.AdjustmentRequest;
import org.example.order.order.application.model.order.request.InventoryAdjustmentTransactionChangeRequest;
import org.example.order.order.application.model.order.request.InventoryTransactionLineItemRequest;
import org.example.order.order.application.service.fulfillment.FulfillmentOrderWriteService;
import org.example.order.order.application.service.orderedit.OrderCommitService;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderLineItem;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;
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
    private final OrderRepository orderRepository;

    private final ApplicationEventPublisher eventPublisher;


    public static final Comparator<FulfillmentOrder> FULFILLMENT_ORDER_COMPARATOR = Comparator
            // sort theo status, nếu close thì để xuống dưới
            .comparing(FulfillmentOrder::getStatus, (s1, s2) -> {
                if (s1 == s2) return 0;
                return s1 == FulfillmentOrder.FulfillmentOrderStatus.closed ? 1 : -1;
            })
            // sort theo location id từ bé đến lớn
            .thenComparingLong(FulfillmentOrder::getAssignedLocationId)
            // sort theo id, status != close => id:asc, status == close => id:desc
            .thenComparing((o1, o2) -> {
                var idCompareResult = Long.compare(o1.getId().getId(), o2.getId().getId());
                return o1.getStatus() != FulfillmentOrder.FulfillmentOrderStatus.closed
                        ? idCompareResult : -idCompareResult;
            });

    /**
     * - Nếu không có line_item nào được restock thì return luôn
     */
    @EventListener(RefundCreatedAppEvent.class)
    public void handleRefundRestockEvent(RefundCreatedAppEvent event) {
        log.debug("handle order restocked fulfillment order: {}", event);
        if (CollectionUtils.isEmpty(event.getRestockLineItems())) {
            return;
        }

        var fulfillmentOrders = fulfillmentOrderRepository.findByOrderId(event.getStoreId(), (int) event.getOrderId())
                .stream()
                .filter(fo -> fo.getLineItems().stream().anyMatch(item -> item.getRemainingQuantity() > 0))
                .sorted(FULFILLMENT_ORDER_COMPARATOR)
                .toList();
        if (CollectionUtils.isEmpty(fulfillmentOrders)) {
            return;
        }

        List<InventoryAdjustmentItem> restockedItems = new ArrayList<>();
        List<InventoryAdjustmentItem> removedItems = new ArrayList<>();

        Map<Integer, Integer> inventoryItemIdMap = new HashMap<>();
        boolean hasRestocked = event.getRestockLineItems().stream()
                .anyMatch(RefundCreatedAppEvent.RestockLineItem::isRestock);
        if (hasRestocked) {
            inventoryItemIdMap = fulfillmentOrders.stream()
                    .flatMap(ffo -> ffo.getLineItems().stream())
                    .filter(foLine -> NumberUtils.isPositive(foLine.getInventoryItemId()))
                    .collect(Collectors.toMap(
                            FulfillmentOrderLineItem::getLineItemId,
                            FulfillmentOrderLineItem::getInventoryItemId,
                            (left, right) -> left
                    ));
        }

        for (var restockLineItem : event.getRestockLineItems()) {
            if (restockLineItem.isRemoval() && !restockLineItem.isRestock()) {
                continue;
            }

            if (restockLineItem.isRestock()) {
                var inventoryItemId = inventoryItemIdMap.get(restockLineItem.lineItemId());
                if (inventoryItemId == null) {
                    continue;
                }
                restockedItems.add(new InventoryAdjustmentItem(
                        restockLineItem.lineItemId(),
                        inventoryItemId,
                        restockLineItem.locationId(),
                        restockLineItem.quantity()
                ));
            }

            if (restockLineItem.isRemoval()) {
                int remainingQuantity = restockLineItem.quantity();
                for (var restockFulfillmentOrder : fulfillmentOrders) {
                    if (remainingQuantity <= 0)
                        break;
                    var restockFulfillmentOrderLineItem = restockFulfillmentOrder.getLineItems().stream()
                            .filter(line -> line.getLineItemId() == restockLineItem.lineItemId())
                            .findFirst()
                            .orElse(null);
                    if (restockFulfillmentOrderLineItem == null)
                        continue;
                    int quantity = Math.min(remainingQuantity, restockFulfillmentOrderLineItem.getRemainingQuantity());
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

        Map<FulfillmentOrderId, FulfillmentOrder> updatedFulfillmentOrders = new HashMap<>();
        for (var restockItem : event.getRestockLineItems()) {
            int remainingQuantity = restockItem.quantity();
            for (var restockFulfillmentOrder : fulfillmentOrders) {
                int quantity = restockFulfillmentOrder.restock(restockItem.lineItemId(), remainingQuantity);
                if (quantity > 0) {
                    remainingQuantity -= quantity;
                    updatedFulfillmentOrders.put(restockFulfillmentOrder.getId(), restockFulfillmentOrder);
                }
            }
        }

        if (!updatedFulfillmentOrders.isEmpty()) {
            updatedFulfillmentOrders.values().forEach(fulfillmentOrderRepository::save);
        }

        if (!restockedItems.isEmpty() || !removedItems.isEmpty()) {

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

    @TransactionalEventListener(classes = {FulfillmentOrderWriteService.FulfillmentOrderMovedAppEvent.class}, phase = TransactionPhase.BEFORE_COMMIT)
    public void handleFulfillmentOrderMoved(FulfillmentOrderWriteService.FulfillmentOrderMovedAppEvent event) {
        log.debug("Handle fulfillment order moved: {}", event);

        var originalFulfillmentOrderId = event.originalFulfillmentOrderId();
        var originalFulfillmentOrder = fulfillmentOrderRepository.findById(originalFulfillmentOrderId)
                .orElseThrow(NotFoundException::new);

        var movedFulfillmentOrderId = event.movedFulfillmentOrderId();
        var movedFulfillmentOrder = fulfillmentOrderRepository.findById(movedFulfillmentOrderId)
                .orElseThrow(NotFoundException::new);

        int storeId = originalFulfillmentOrderId.getStoreId();
        int orderId = originalFulfillmentOrder.getOrderId();
        var order = orderRepository.findById(new OrderId(storeId, orderId));

        InventoryRequest inventoryRequest = null;

    }

    @EventListener(OrderCommitService.OrderEditedAppEvent.class)
    public void handleOrderEditingEvent(OrderCommitService.OrderEditedAppEvent event) {

    }
}
