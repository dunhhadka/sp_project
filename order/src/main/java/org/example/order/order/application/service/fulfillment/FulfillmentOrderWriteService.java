package org.example.order.order.application.service.fulfillment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.SapoClient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.fulfillmentorder.request.FulfillmentOrderMoveRequest;
import org.example.order.order.application.model.fulfillmentorder.response.FulfillmentOrderMovedResponse;
import org.example.order.order.application.model.fulfillmentorder.response.LocationForMove;
import org.example.order.order.application.model.order.context.OrderCreatedEvent;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.response.OrderRoutingResponse;
import org.example.order.order.application.service.fulfillmentorder.FulfillmentOrderHelperService;
import org.example.order.order.application.service.fulfillmentorder.MovedFulfillmentOrderEvent;
import org.example.order.order.domain.fulfillmentorder.model.*;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.*;
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

    private final FulfillmentOrderIdGenerator idGenerator;

    private final OrderRepository orderRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;

    private final FulfillmentOrderHelperService fulfillmentOrderHelperService;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final SapoClient sapoClient;

    @Transactional
    @EventListener(classes = {OrderCreatedEvent.class})
    public void handleOrderFulfillmentAdded(OrderCreatedEvent event) {
        log.debug("handle fulfillment_order added {}", event);
        var storeId = event.getStore().getId();
        var isWithFulfillment = CollectionUtils.isNotEmpty(event.getFulfillmentRequests());

        var order = orderRepository.findById(event.getOrderId());
        var orderRoutingResponse = event.getOrderRoutingResponse();
        var shippingAddress = order.getShippingAddress(); // Địa chỉ nhận hàng

        List<FulfillmentOrder> fulfillmentOrderList = new ArrayList<>();

        for (var routingResult : orderRoutingResponse.getOrderRoutingResults()) {
            FulfillmentOrder fulfillmentOrder;

            var location = routingResult.getLocation();
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
                        .email(Optional.ofNullable(order.getCustomerInfo()).map(CustomerInfo::getEmail).orElse(null))
                        .address(shippingAddress.getAddressInfo().getAddress())
                        .build();
            }

            Instant fulfillOn = order.getCreatedOn();

            var expectedDeliveryInfo = determineExpectedDeliveryInfo(order, isWithFulfillment);
            var requireShipping = expectedDeliveryInfo.getLeft();
            var expectedDeliveryMethod = expectedDeliveryInfo.getRight();
            if (requireShipping) {
                fulfillmentOrder = newFulfillmentOrder(assignedLocation, destination, order,
                        true, expectedDeliveryMethod, location, fulfillOn);
            } else {
                fulfillmentOrder = newFulfillmentOrder(assignedLocation, destination, order,
                        false, expectedDeliveryMethod, location, fulfillOn);
            }

            for (int i = 0; i < routingResult.getIndexesItems().size(); i++) {
                var item = routingResult.getIndexesItems().get(i);
                var orderLineItem = order.getLineItems().get(item.getIndex());

                VariantInfo variantInfo = orderLineItem.getVariantInfo();
                Objects.requireNonNull(variantInfo); // continue?
                ProductVariantInfo productVariantInfo = ProductVariantInfo.builder()
                        .productId(variantInfo.getProductId())
                        .variantTitle(variantInfo.getVariantTitle())
                        .title(variantInfo.getTitle())
                        .sku(variantInfo.getSku())
                        .grams((float) variantInfo.getGrams())
                        .vendor(variantInfo.getVendor())
                        .price(orderLineItem.getPrice())
                        .discountedUnitPrice(orderLineItem.getDiscountUnitPrice())
                        .build();
                fulfillmentOrder.addLineItem(order.getId(), orderLineItem.getId(),
                        (long) item.getInventoryItemId(), variantInfo.getVariantId(),
                        productVariantInfo, orderLineItem.getQuantity());
            }

            fulfillmentOrderList.add(fulfillmentOrder);
        }

        fulfillmentOrderList.forEach(fulfillmentOrder -> {
            if (isWithFulfillment) {
                fulfillmentOrder.markAsFulfilled(Collections.emptyList());
            }
            fulfillmentOrderRepository.save(fulfillmentOrder);
        });

        var fulfillmentOrderIds = fulfillmentOrderList.stream().map(l -> l.getId().getId()).toList();

        applicationEventPublisher.publishEvent(
                new FulfillmentOrderListCreatedAppEvent(
                        storeId, order.getId().getId(),
                        fulfillmentOrderIds, isWithFulfillment, event.getFulfillmentRequests())
        );
    }

    @Transactional
    public FulfillmentOrderMovedResponse move(FulfillmentOrderId fulfillmentOrderId, FulfillmentOrderMoveRequest request) {
        var originalFulfillmentOrder = findFFOById(fulfillmentOrderId);

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
                    .fields(List.of("status", "request_status"))
                    .message("cannot move location for fulfillment_order")
                    .build());
        }

        int storeId = fulfillmentOrderId.getStoreId();

        var order = orderRepository.findById(new OrderId(storeId, originalFulfillmentOrder.getOrderId()));
        if (order == null) {
            throw new NotFoundException("cannot find order with id = " + originalFulfillmentOrder.getOrderId());
        }

        if (CollectionUtils.isNotEmpty(request.getFulfillmentOrderLineItems())) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_supported")
                    .fields(List.of("fulfillment_line_items"))
                    .message("not support for move fulfillment_line_item")
                    .build());
        }

        var originalLocationId = originalFulfillmentOrder.getAssignedLocationId();
        var newLocationId = request.getNewLocationId();

        List<LocationForMove> locationForMoves = fulfillmentOrderHelperService.getLocationForMove(fulfillmentOrderId);

        var locationForMove = locationForMoves.stream()
                .filter(l -> l.getLocation().getId() == newLocationId)
                .findFirst()
                .orElseThrow(() -> new ConstrainViolationException(UserError.builder()
                        .code("not_allowed")
                        .fields(List.of("location_id"))
                        .message("")
                        .build()));

        if (!locationForMove.isMovable()) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_allowed")
                    .fields(List.of("location_id"))
                    .message(locationForMove.getMessage())
                    .build()
            );
        }

        Location location = findLocationById(newLocationId, storeId);

        AssignedLocation newAssignLocation = AssignedLocation.builder()
                .name(location.getName())
                .phone(location.getPhone())
                .email(location.getEmail())
                .address(location.getAddress())
                .build();

        if (existingNewLocationMoved(originalFulfillmentOrder, newLocationId)) {
            return new FulfillmentOrderMovedResponse(originalFulfillmentOrder, null);
        }

        var movedFulfillmentOrder = originalFulfillmentOrder.move(newLocationId, newAssignLocation);

        fulfillmentOrderRepository.save(originalFulfillmentOrder);
        if (!Objects.equals(movedFulfillmentOrder.getAssignedLocationId(), originalFulfillmentOrder.getAssignedLocationId())) {
            fulfillmentOrderRepository.save(movedFulfillmentOrder);
        }

        applicationEventPublisher.publishEvent(
                new MovedFulfillmentOrderEvent(originalFulfillmentOrder.getId(), movedFulfillmentOrder.getId(),
                        originalLocationId, newLocationId));

        return new FulfillmentOrderMovedResponse(originalFulfillmentOrder, movedFulfillmentOrder);
    }

    private boolean existingNewLocationMoved(FulfillmentOrder originalFulfillmentOrder, Long newLocationId) {
        return false;
    }

    // Tìm location trong db
    private Location findLocationById(Long newLocationId, Integer storeId) {
        return sapoClient.location(LocationFilter.builder().id(newLocationId).build());
    }

    private FulfillmentOrder findFFOById(FulfillmentOrderId fulfillmentOrderId) {
        return fulfillmentOrderRepository.findById(fulfillmentOrderId)
                .orElseThrow(() -> new NotFoundException("fulfillment_order not found"));
    }

    public record FulfillmentOrderListCreatedAppEvent(
            int storeId, int orderId,
            List<Integer> fulfillmentOrderIds, boolean isWithFulfillment,
            List<OrderCreateRequest.FulfillmentRequest> fulfillmentRequests
    ) {

    }

    private FulfillmentOrder newFulfillmentOrder(
            AssignedLocation assignedLocation, Destination destination,
            Order order, Boolean requireShipping,
            FulfillmentOrder.ExpectedDeliveryMethod expectedDeliveryMethod,
            OrderRoutingResponse.OrderRoutingLocation location, Instant fulfillOn
    ) {
        return new FulfillmentOrder(
                idGenerator, order.getId(),
                location.getId(),
                assignedLocation,
                expectedDeliveryMethod,
                requireShipping,
                destination,
                fulfillOn
        );
    }

    private Pair<Boolean, FulfillmentOrder.ExpectedDeliveryMethod> determineExpectedDeliveryInfo(Order order, boolean isWithFulfillment) {
        var shippingCodes = Optional.ofNullable(order.getShippingLines()).stream()
                .flatMap(Collection::stream)
                .map(ShippingLine::getCode)
                .toList();
        if (CollectionUtils.isEmpty(shippingCodes) && isWithFulfillment) {
            return Pair.of(false, FulfillmentOrder.ExpectedDeliveryMethod.retail);
        }

        boolean requireShipping = order.getLineItems().stream()
                .anyMatch(lineItem -> lineItem.getVariantInfo().isRequireShipping());
        var expectedDeliveryMethod = requireShipping ? FulfillmentOrder.ExpectedDeliveryMethod.shipping : FulfillmentOrder.ExpectedDeliveryMethod.none;
        return Pair.of(requireShipping, expectedDeliveryMethod);
    }
}
