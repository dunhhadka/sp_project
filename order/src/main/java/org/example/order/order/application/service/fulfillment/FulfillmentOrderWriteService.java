package org.example.order.order.application.service.fulfillment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.fulfillmentorder.request.FulfillmentOrderMoveRequest;
import org.example.order.order.application.model.fulfillmentorder.response.FulfillmentOrderMovedResponse;
import org.example.order.order.application.model.fulfillmentorder.response.LocationForMove;
import org.example.order.order.application.model.order.context.OrderCreatedEvent;
import org.example.order.order.application.model.order.response.OrderRoutingResponse;
import org.example.order.order.application.service.fulfillmentorder.FulfillmentOrderHelperService;
import org.example.order.order.domain.fulfillmentorder.model.*;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.infrastructure.data.dto.Location;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentOrderWriteService {

    private final OrderRepository orderRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final FulfillmentOrderHelperService fulfillmentOrderHelperService;

    private final ApplicationEventPublisher eventPublisher;

    @EventListener(classes = OrderCreatedEvent.class)
    public void handleFulfillmentOrderAdded(OrderCreatedEvent event) {
        log.debug("Handle order fulfillment order added: {}", event);

        var storeId = event.getStore().getId();
        var order = orderRepository.findById(event.getOrderId());
        var orderRoutingResponse = event.getOrderRoutingResponse();
        var shippingAddress = order.getShippingAddress();

        List<FulfillmentOrder> fulfillmentOrderList = new ArrayList<>();
        for (var routing : orderRoutingResponse.getOrderRoutingResults()) {
            FulfillmentOrder requiredShippingFulfillmentOrder = null;
            FulfillmentOrder nonRequireShippingFulfillmentOrder = null;

            var location = routing.getLocation();
            AssignedLocation assignedLocation = AssignedLocation.builder()
                    .name(location.getName())
                    .phone(location.getPhone())
                    .email(location.getEmail())
                    .address(location.getAddress())
                    .build();

            Destination destination = null;
            if (shippingAddress != null) {
                destination = Destination.builder()
                        .firstName(shippingAddress.getAddressInfo().getFirstName())
                        .lastName(shippingAddress.getAddressInfo().getLastName())
                        .phone(shippingAddress.getAddressInfo().getPhone())
                        .address(shippingAddress.getAddressInfo().getAddress())
                        .build();
            }

            Instant fulfillOn = Instant.now();
            for (int i = 0; i < routing.getIndexesItems().size(); i++) {
                var item = routing.getIndexesItems().get(i);
                var orderLineItem = order.getLineItems().get(item.getIndex());

                ProductVariantInfo productVariantInfo = Optional.ofNullable(orderLineItem.getVariantInfo())
                        .map(variant ->
                                ProductVariantInfo.builder()
                                        .productId(variant.getProductId())
                                        .variantTitle(variant.getVariantTitle())
                                        .title(variant.getTitle())
                                        .sku(variant.getSku())
                                        .grams((float) variant.getGrams())
                                        .vendor(variant.getVendor())
                                        .price(orderLineItem.getPrice())
                                        .build())
                        .orElse(null);

                var expectedDeliveryInfo = determineDeliveryInfo();
                var requiredShipping = expectedDeliveryInfo.getLeft();
                if (requiredShipping) {
                    requiredShippingFulfillmentOrder = newFulfillmentOrder(requiredShippingFulfillmentOrder, order,
                            routing, assignedLocation, destination);
                    requiredShippingFulfillmentOrder.addLineItem(
                            order.getId(),
                            orderLineItem.getId(),
                            orderLineItem.getVariantInfo().getInventoryItemId(),
                            orderLineItem.getVariantInfo().getVariantId(),
                            productVariantInfo,
                            orderLineItem.getQuantity()
                    );
                } else {
                    nonRequireShippingFulfillmentOrder = newFulfillmentOrder(nonRequireShippingFulfillmentOrder, order,
                            routing, assignedLocation, destination);
                    nonRequireShippingFulfillmentOrder.addLineItem(
                            order.getId(),
                            orderLineItem.getId(),
                            orderLineItem.getVariantInfo().getInventoryItemId(),
                            orderLineItem.getVariantInfo().getVariantId(),
                            productVariantInfo,
                            orderLineItem.getQuantity()
                    );
                }
            }

            Optional.ofNullable(requiredShippingFulfillmentOrder).ifPresent(fulfillmentOrderList::add);
            Optional.ofNullable(nonRequireShippingFulfillmentOrder).ifPresent(fulfillmentOrderList::add);
        }

        if (CollectionUtils.isNotEmpty(fulfillmentOrderList)) {
            fulfillmentOrderList.forEach(ffo -> ffo.markAsFulfilled(Collections.emptyList()));
        }
    }

    private FulfillmentOrder newFulfillmentOrder(FulfillmentOrder requiredShippingFulfillmentOrder, Order order, OrderRoutingResponse.OrderRoutingResult routing, AssignedLocation assignedLocation, Destination destination) {
        if (requiredShippingFulfillmentOrder != null) {
            return requiredShippingFulfillmentOrder;
        }

        return null;
    }

    private Pair<Boolean, Boolean> determineDeliveryInfo() {
        return null;
    }


    @Transactional
    public FulfillmentOrderMovedResponse move(FulfillmentOrderId id, FulfillmentOrderMoveRequest request) {
        var originalFulfillmentOrder = findFFOById(id);

        var validStates = List.of(
                Pair.of(FulfillmentOrder.FulfillmentOrderStatus.open, FulfillmentOrder.FulfillmentOrderRequestStatus.unsubmitted),
                Pair.of(FulfillmentOrder.FulfillmentOrderStatus.open, FulfillmentOrder.FulfillmentOrderRequestStatus.rejected),
                Pair.of(FulfillmentOrder.FulfillmentOrderStatus.in_progress, FulfillmentOrder.FulfillmentOrderRequestStatus.unsubmitted),
                Pair.of(FulfillmentOrder.FulfillmentOrderStatus.in_progress, FulfillmentOrder.FulfillmentOrderRequestStatus.rejected),
                Pair.of(FulfillmentOrder.FulfillmentOrderStatus.cancelled, FulfillmentOrder.FulfillmentOrderRequestStatus.cancellation_accepted),
                Pair.of(FulfillmentOrder.FulfillmentOrderStatus.incomplete, FulfillmentOrder.FulfillmentOrderRequestStatus.closed)
        );

        if (!validStates.contains(Pair.of(originalFulfillmentOrder.getStatus(), originalFulfillmentOrder.getRequestStatus()))) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_allowed")
                    .message("")
                    .build());
        }

        var order = orderRepository.findById(new OrderId(id.getStoreId(), originalFulfillmentOrder.getOrderId()));
        if (order == null) {
            throw new ConstrainViolationException(UserError.builder()
                    .build());
        }

        if (CollectionUtils.isNotEmpty(request.getFulfillmentOrderLineItems())) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_supported")
                    .fields(List.of("fulfillment_order_line_items"))
                    .build());
        }

        var newLocationId = request.getNewLocationId();

        List<LocationForMove> locationForMoves = fulfillmentOrderHelperService.getMovableLocations(id);
        var locationForMove = locationForMoves.stream()
                .filter(location -> Objects.equals((long) location.getLocation().getId(), newLocationId))
                .findFirst()
                .orElseThrow(() -> new ConstrainViolationException(UserError.builder()
                        .message("can't find movable location for request")
                        .fields(List.of("location_id"))
                        .build()));
        if (!locationForMove.isMovable()) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_allowed")
                    .fields(List.of("location_id"))
                    .message(locationForMove.getMessage())
                    .build());
        }

        Location location = findLocationById(locationForMove.getLocation().getId());
        AssignedLocation newLocation = AssignedLocation.builder()
                .name(location.getName())
                .phone(location.getPhone())
                .email(location.getEmail())
                .address(location.getAddress())
                .build();

        var movedFulfillmentOrder = originalFulfillmentOrder.move(newLocationId, newLocation);
        if (!Objects.equals(movedFulfillmentOrder.getAssignedLocationId(), originalFulfillmentOrder.getAssignedLocationId())) {
            fulfillmentOrderRepository.save(movedFulfillmentOrder);
        }

        eventPublisher.publishEvent(new FulfillmentOrderMovedAppEvent(originalFulfillmentOrder.getId(), movedFulfillmentOrder.getId()));

        return new FulfillmentOrderMovedResponse(originalFulfillmentOrder, movedFulfillmentOrder);
    }

    public record FulfillmentOrderMovedAppEvent(
            FulfillmentOrderId originalFulfillmentOrderId,
            FulfillmentOrderId movedFulfillmentOrderId
    ) {
    }

    private boolean existingFulfillmentOrder(FulfillmentOrder originalFulfillmentOrder, Long newLocationId) {
        return false;
    }

    private Location findLocationById(int id) {
        return new Location();
    }

    private FulfillmentOrder findFFOById(FulfillmentOrderId id) {
        return fulfillmentOrderRepository.findById(id)
                .orElseThrow(() ->
                        new ConstrainViolationException(UserError.builder()
                                .message("fulfillment_order not found")
                                .build()));
    }
}