package org.example.order.domain.order.model;

import org.example.order.infrastructure.persistence.InMemoryIdGenerator;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.infrastructure.data.dto.StoreDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

public interface OrderFixtures {

    // region shared values

    default Order defaultOrder() {
        return new Order(storeId, processAt, customer, tracking, currency,
                totalWeight, note, tags, billingAddress, shippingAddress, lineItems,
                shippingLines, discountCodes, discountAllocations, discountApplications, taxExempt,
                taxesIncluded, processingMethod, gateWay, idGenerator, locationId, combinationLines);
    }

    default StoreDto defaultStore() {
        return StoreDto.builder()
                .id(storeId)
                .name("Store Thanh Hóa")
                .build();
    }

    int storeId = 1;
    Instant processAt = Instant.now();
    CustomerInfo customer = new CustomerInfo("email", "phone", 1, true);
    TrackingInfo tracking = new TrackingInfo("source", null, null, null, null, null, null, null);
    Currency currency = Currency.getInstance("VND");
    int totalWeight = 1;
    String note = "note";
    List<String> tags = List.of("tag1", "tag2");

    BillingAddress billingAddress = new BillingAddress(1, MailingAddress.builder()
            .firstName("John")
            .lastName("Doe")
            .phone("0982345678")
            .province("TP Hồ Chí Minh")
            .provinceCode("2")
            .countryCode("VN")
            .district("Quận 11")
            .districtCode("40")
            .ward("Phường 15")
            .wardCode("9380")
            .build());
    ShippingAddress shippingAddress = new ShippingAddress(1, MailingAddress.builder()
            .city("Ha noi")
            .phone("0906907234")
            .province("Hà Nội")
            .provinceCode("1")
            .countryCode("VN")
            .district("Quận Ba Đình")
            .districtCode("2")
            .ward("Phường Cống vị")
            .wardCode("4")
            .build());

    int lineItem1Id = 12345;
    int lineItem2Id = 67890;
    int lineItem3Id = 1357;
    int lineItem1Quantity = 1;
    int lineItem2Quantity = 2;
    int lineItem3Quantity = 3;
    int inventoryItem1Id = 123;
    int inventoryItem2Id = 456;
    int inventoryItem3Id = 789;
    int product1Id = 12;
    int variant1Id = 10;
    int product2Id = 13;
    int variant2Id = 11;
    int product3Id = 14;
    int variant3Id = 12;
    BigDecimal lineItem1Price = BigDecimal.valueOf(100000);
    BigDecimal lineItem2Price = BigDecimal.valueOf(150000);
    BigDecimal lineItem3Price = BigDecimal.valueOf(158000);

    boolean taxExempt = false;
    boolean taxesIncluded = true;

    List<LineItem> lineItems = List.of(
            new LineItem(lineItem1Id, lineItem1Quantity, lineItem1Price, BigDecimal.valueOf(5000), "discountCode",
                    new VariantInfo(product1Id, variant1Id, true, "product_1", "product_1_title", "variant_1_title", "vendor", "sku", 5, true,
                            "management", true, (long) inventoryItem1Id, "unit"), false,
                    List.of(new TaxLine(1, BigDecimal.valueOf(0.2), "VAT", BigDecimal.valueOf(2000), lineItem1Id, null, 1)),
                    "service", false, null),
            new LineItem(lineItem2Id, lineItem2Quantity, lineItem2Price, BigDecimal.valueOf(1000), "discountCode",
                    new VariantInfo(product1Id, variant1Id, true, "product_2", "product_2_title", "variant_2_title", "vendor", "sku", 5, true,
                            "management", true, (long) inventoryItem2Id, "unit"), false,
                    List.of(new TaxLine(2, BigDecimal.valueOf(0.1), "VAT", BigDecimal.valueOf(2000), lineItem2Id, null, 1)),
                    "service", false, null),
            new LineItem(lineItem1Id, lineItem1Quantity, lineItem3Price, BigDecimal.valueOf(5000), "discountCode",
                    new VariantInfo(product1Id, variant1Id, true, "product_3", "product_3_title", "variant_3_title", "vendor", "sku", 5, true,
                            "management", true, (long) inventoryItem3Id, "unit"), true,
                    List.of(), "service", true, null)
    );

    List<ShippingLine> shippingLines = List.of();

    List<OrderDiscountCode> discountCodes = Collections.emptyList();
    List<DiscountApplication> discountApplications = Collections.emptyList();
    List<DiscountAllocation> discountAllocations = Collections.emptyList();

    String processingMethod = "";
    String gateWay = "";
    OrderIdGenerator idGenerator = new InMemoryIdGenerator();
    Long locationId = 1L;
    List<CombinationLine> combinationLines = List.of();
    // endregion shared values
}
