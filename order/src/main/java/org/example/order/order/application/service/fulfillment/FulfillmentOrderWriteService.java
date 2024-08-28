package org.example.order.order.application.service.fulfillment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.model.order.context.OrderCreatedEvent;
import org.example.order.order.application.model.order.response.OrderRoutingResponse;
import org.example.order.order.domain.fulfillmentorder.model.*;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.ShippingLine;
import org.example.order.order.domain.order.model.VariantInfo;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentOrderWriteService {

    private final FulfillmentOrderIdGenerator idGenerator;

    private final OrderRepository orderRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;


    @Transactional
    @EventListener(classes = {OrderCreatedEvent.class})
    public void handleOrderFulfillmentAdded(OrderCreatedEvent event) {
        log.debug("handle order fulfillment added: {}", event);
        var isWithFulfillment = !CollectionUtils.isEmpty(event.getFulfillmentRequests());

        var order = orderRepository.findById(event.getOrderId());
        var orderRoutingResponse = event.getOrderRoutingResponse();
        var shippingAddress = order.getShippingAddress();

        List<FulfillmentOrder> fulfillmentOrders = new ArrayList<>();
        for (var routingResult : orderRoutingResponse.getOrderRoutingResults()) {
            FulfillmentOrder requireShippingFulfillmentOrder = null;
            FulfillmentOrder nonRequireShippingFulfillmentOrder = null;

            var location = routingResult.getLocation();
            AssignedLocation assignedLocation = AssignedLocation.builder()
                    .name(location.getName())
                    .email(location.getEmail())
                    .phone(location.getPhone())
                    .address(location.getAddress())
                    .build();

            Destination destination = null;
            if (shippingAddress != null) {
                destination = Destination.builder()
                        .firstName(shippingAddress.getAddressInfo().getFirstName())
                        .lastName(shippingAddress.getAddressInfo().getLastName())
                        .phone(shippingAddress.getAddressInfo().getPhone())
                        .email(order.getCustomerInfo() == null ? null : order.getCustomerInfo().getEmail())
                        .address(shippingAddress.getAddressInfo().getAddress())
                        .build();
            }

            Instant fulfillOn = Instant.now();

            for (int i = 0; i < routingResult.getIndexesItems().size(); i++) {
                var item = routingResult.getIndexesItems().get(i);
                var orderLineItem = order.getLineItems().get(item.getIndex());

                VariantInfo variantInfo = orderLineItem.getVariantInfo();
                Integer variantId = orderLineItem.getVariantInfo().getVariantId();
                ProductVariantInfo productVariantInfo = ProductVariantInfo.builder()
                        .productId(variantInfo.getProductId())
                        .variantTitle(variantInfo.getVariantTitle())
                        .title(variantInfo.getTitle())
                        .sku(variantInfo.getSku())
                        .grams((float) variantInfo.getGrams())
                        .vendor(variantInfo.getVendor())
                        .image(null)
                        .price(orderLineItem.getPrice())
                        .discountedUnitPrice(orderLineItem.getDiscountUnitPrice())
                        .build();

                var expectedDeliveryInfo = determineExpectedDelivery(order, orderLineItem, isWithFulfillment);
                var requireShipping = expectedDeliveryInfo.getLeft();
                var expectedDeliveryMethod = expectedDeliveryInfo.getRight();

                if (requireShipping) {
                    requireShippingFulfillmentOrder = newFulfillmentOrder(requireShippingFulfillmentOrder, order,
                            routingResult, assignedLocation, destination, true, expectedDeliveryMethod, fulfillOn);
                    requireShippingFulfillmentOrder.addLineItem(
                            order.getId(),
                            orderLineItem.getId(),
                            variantInfo.getInventoryItemId(),
                            variantId,
                            productVariantInfo,
                            orderLineItem.getQuantity()
                    );
                } else {
                    nonRequireShippingFulfillmentOrder = newFulfillmentOrder(nonRequireShippingFulfillmentOrder, order,
                            routingResult, assignedLocation, destination, false, expectedDeliveryMethod, fulfillOn);
                    nonRequireShippingFulfillmentOrder.addLineItem(
                            order.getId(),
                            orderLineItem.getId(),
                            variantInfo.getInventoryItemId(),
                            variantId,
                            productVariantInfo,
                            orderLineItem.getQuantity()
                    );
                }
            }

            Optional.ofNullable(requireShippingFulfillmentOrder).ifPresent(fulfillmentOrders::add);
            Optional.ofNullable(nonRequireShippingFulfillmentOrder).ifPresent(fulfillmentOrders::add);
        }

        fulfillmentOrders.forEach(fulfillmentOrder -> {
            if (isWithFulfillment) {
                fulfillmentOrder.markFulfilled(List.of());
            }
            fulfillmentOrderRepository.save(fulfillmentOrder);
        });
    }

    private FulfillmentOrder newFulfillmentOrder(
            FulfillmentOrder fulfillmentOrder,
            Order order,
            OrderRoutingResponse.OrderRoutingResult routingResult,
            AssignedLocation assignedLocation,
            Destination destination,
            boolean requireShipping,
            FulfillmentOrder.ExpectedDeliveryMethod expectedDeliveryMethod,
            Instant fulfillOn
    ) {
        return Optional.ofNullable(fulfillmentOrder).orElse(
                new FulfillmentOrder(
                        idGenerator,
                        order.getId(),
                        routingResult.getLocation().getId(),
                        assignedLocation,
                        expectedDeliveryMethod,
                        requireShipping,
                        destination,
                        fulfillOn
                )
        );
    }

    private Pair<Boolean, FulfillmentOrder.ExpectedDeliveryMethod> determineExpectedDelivery(Order order, LineItem orderLineItem, boolean isWithFulfillment) {
        var shippingLineCodes = Optional.ofNullable(order.getShippingLines()).stream()
                .flatMap(Collection::stream)
                .map(ShippingLine::getCode)
                .toList();
        var requireShipping = orderLineItem.getVariantInfo().isRequireShipping();
        var expectedDeliveryMethod = requireShipping ? FulfillmentOrder.ExpectedDeliveryMethod.shipping : FulfillmentOrder.ExpectedDeliveryMethod.none;
        return Pair.of(requireShipping, expectedDeliveryMethod);
    }
}
