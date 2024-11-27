package org.example.order.domain.fulfillmentorder.model;

import org.example.order.domain.order.model.OrderFixtures;
import org.example.order.infrastructure.persistence.InMemoryIdGenerator;
import org.example.order.order.application.model.fulfillmentorder.response.LocationForMove;
import org.example.order.order.domain.fulfillmentorder.model.*;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.infrastructure.data.dto.Location;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface FulfillmentOrderFixtures extends OrderFixtures {
    FulfillmentOrderIdGenerator idGenerator = new InMemoryIdGenerator();
    Integer storeId = 1;
    Long assignedLocationId = 123L;
    FulfillmentOrder.ExpectedDeliveryMethod expectedDeliveryMethod = FulfillmentOrder.ExpectedDeliveryMethod.external_service;
    boolean requireShipping = true;
    Instant fulfillOn = Instant.now();
    AssignedLocation assignedLocation = AssignedLocation.builder()
            .name("Kho Đội Cấn")
            .phone("0836023098")
            .email("gianglt@sapo.vn")
            .address("266 Đội Cấn")
            .build();
    Destination destinationLocation = Destination.builder()
            .firstName("John")
            .lastName("Doe")
            .phone("0836023098")
            .address("Thanh Hóa")
            .build();

    OrderId orderId = new OrderId(storeId, 1);
    Integer lineItem1Id = OrderFixtures.lineItem1Id;
    Integer lineItem2Id = OrderFixtures.lineItem2Id;
    Integer lineItem3Id = OrderFixtures.lineItem3Id;
    Integer inventoryItem1Id = OrderFixtures.inventoryItem1Id;
    Integer inventoryItem2Id = OrderFixtures.inventoryItem2Id;
    Integer inventoryItem3Id = OrderFixtures.inventoryItem2Id;
    Integer variant1Id = OrderFixtures.variant1Id;
    Integer variant2Id = OrderFixtures.variant2Id;
    Integer variant3Id = OrderFixtures.variant3Id;
    Integer quantity1Id = OrderFixtures.lineItem1Quantity;
    Integer quantity2Id = OrderFixtures.lineItem2Quantity;
    Integer quantity3Id = OrderFixtures.lineItem3Quantity;
    Integer product1Id = OrderFixtures.product1Id;
    Integer product2Id = OrderFixtures.product2Id;
    Integer product3Id = OrderFixtures.product3Id;
    ProductVariantInfo productVariant1 = ProductVariantInfo.builder()
            .productId(product1Id)
            .variantTitle("variant_1_title")
            .title("title1")
            .sku("sku")
            .grams(150F)
            .vendor("vendor")
            .price(BigDecimal.valueOf(100000))
            .discountedUnitPrice(BigDecimal.valueOf(90000))
            .build();
    ProductVariantInfo productVariant2 = ProductVariantInfo.builder()
            .productId(product2Id)
            .variantTitle("variant_2_title")
            .title("title2")
            .sku("sku")
            .grams(50F)
            .vendor("vendor")
            .price(BigDecimal.valueOf(150000))
            .discountedUnitPrice(BigDecimal.valueOf(150000))
            .build();
    ProductVariantInfo productVariant3 = ProductVariantInfo.builder()
            .productId(product3Id)
            .variantTitle("variant_3_title")
            .title("title3")
            .sku("sku")
            .grams(8F)
            .vendor("vendor")
            .price(BigDecimal.valueOf(500000))
            .discountedUnitPrice(BigDecimal.valueOf(490000))
            .build();

    default FulfillmentOrder defaultFulfillmentOrder() {
        var fulfillmentOrder = new FulfillmentOrder(
                idGenerator, orderId, assignedLocationId, assignedLocation,
                expectedDeliveryMethod, requireShipping, destinationLocation, fulfillOn);
        fulfillmentOrder.addLineItem(orderId, lineItem1Id, (long) inventoryItem1Id, variant1Id, productVariant1, quantity1Id);
        fulfillmentOrder.addLineItem(orderId, lineItem2Id, (long) inventoryItem2Id, variant2Id, productVariant2, quantity2Id);
        fulfillmentOrder.addLineItem(orderId, lineItem3Id, (long) inventoryItem3Id, variant3Id, productVariant3, quantity3Id);

        fulfillmentOrder.reopen();
        return fulfillmentOrder;
    }

    default Location destinationLocation() {
        return Location.builder()
                .id(567L)
                .name("Kho HCM")
                .build();
    }

    default List<LocationForMove> locationsForMove() {
        return List.of(
                LocationForMove.builder()
                        .location(LocationForMove.Location.builder()
                                .id(assignedLocationId.intValue())
                                .name(assignedLocation.getName())
                                .build())
                        .movable(false)
                        .message("Current location.")
                        .build(),
                LocationForMove.builder()
                        .location(LocationForMove.Location.builder()
                                .id(234)
                                .name("Kho Nghệ An")
                                .build())
                        .movable(false)
                        .message("No inventory items at this location")
                        .build(),
                LocationForMove.builder()
                        .location(LocationForMove.Location.builder()
                                .id(345)
                                .name("Kho Đà Nẵng")
                                .build())
                        .movable(false)
                        .message("1 item can't be changed because it isn't stocked at this location.")
                        .build(),
                LocationForMove.builder()
                        .location(LocationForMove.Location.builder()
                                .id(456)
                                .name("Kho Bình Dương")
                                .build())
                        .movable(false)
                        .message("2 items can't be changed because they aren't stocked at this location.")
                        .build(),
                LocationForMove.builder()
                        .location(LocationForMove.Location.builder()
                                .id((int) destinationLocation().getId())
                                .name(destinationLocation().getName())
                                .build())
                        .movable(true)
                        .build()

        );
    }

    default FulfillmentOrder partialFulfillmentLocation() {
        var fulfillmentOrder = new FulfillmentOrder(
                idGenerator, orderId, assignedLocationId, assignedLocation,
                expectedDeliveryMethod, requireShipping, destinationLocation, fulfillOn);
        fulfillmentOrder.addLineItem(orderId, lineItem1Id, (long) inventoryItem1Id, variant1Id, productVariant1, quantity1Id);
        fulfillmentOrder.addLineItem(orderId, lineItem2Id, (long) inventoryItem2Id, variant2Id, productVariant2, quantity2Id);
        fulfillmentOrder.addLineItem(orderId, lineItem3Id, (long) inventoryItem3Id, variant3Id, productVariant3, quantity3Id);

        var line1 = fulfillmentOrder.getLineItems().get(0);
        var line2 = fulfillmentOrder.getLineItems().get(1);
        var line3 = fulfillmentOrder.getLineItems().get(2);

        List<FulfillmentOrderLineItemInput> fulfilledRequest = List.of(
                new FulfillmentOrderLineItemInput(line1.getId(), 1),
                new FulfillmentOrderLineItemInput(line2.getId(), 1),
                new FulfillmentOrderLineItemInput(line3.getId(), 2)
        );
        fulfillmentOrder.markAsFulfilled(fulfilledRequest);

        fulfillmentOrder.partialFulfillStatus();
        return fulfillmentOrder;
    }
}
