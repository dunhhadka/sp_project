package org.example.order.application.service.orderedit;

import org.example.order.infrastructure.persistence.InMemoryIdGenerator;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.order.persistence.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

public class TestOrderRepository implements OrderRepository {

    private static final int storeId = 1;

    private static final List<LineItem> lineItems = createLineItems();

    private static List<LineItem> createLineItems() {
        List<TaxLine> emptyTaxLines = Collections.emptyList();

        var variant1 = VariantInfo.builder()
                .productId(1)
                .variantId(1)
                .productExisted(true)
                .name("product 1")
                .title("product title 1")
                .variantTitle("variant title 1")
                .vendor("")
                .sku("")
                .grams(10)
                .requireShipping(true)
                .restockable(true)
                .inventoryItemId(1L)
                .build();
        var variant2 = VariantInfo.builder()
                .productId(2)
                .variantId(2)
                .productExisted(true)
                .name("product 2")
                .title("product title 2")
                .variantTitle("variant title 2")
                .vendor("vendor 2")
                .sku("SKU-002")
                .grams(20)
                .requireShipping(true)
                .restockable(true)
                .inventoryItemId(2L)
                .build();

        var variant3 = VariantInfo.builder()
                .productId(3)
                .variantId(3)
                .productExisted(true)
                .name("product 3")
                .title("product title 3")
                .variantTitle("variant title 3")
                .vendor("vendor 3")
                .sku("SKU-003")
                .grams(30)
                .requireShipping(false)
                .restockable(false)
                .inventoryItemId(3L)
                .build();
        LineItem item1 = new LineItem(
                1,
                10,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                "DISCOUNT10",
                variant1,
                true,
                emptyTaxLines,
                null,
                false,
                null
        );

        LineItem item2 = new LineItem(
                2,
                5,
                new BigDecimal("200.00"),
                new BigDecimal("20.00"),
                "DISCOUNT20",
                variant2,
                true,
                emptyTaxLines,
                null,
                false,
                null
        );

        LineItem item3 = new LineItem(
                3,
                15,
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                "DISCOUNT5",
                variant3,
                false,
                emptyTaxLines,
                null,
                false,
                null
        );

        return List.of(item1, item2, item3);
    }

    private static final Long locationId = 10L;

    public static final Order order =
            new Order(
                    storeId,
                    Instant.now(),
                    CustomerInfo.builder().email("123@gmail.com").phone("0836023098").build(),
                    TrackingInfo.builder().build(),
                    Currency.getInstance("VND"),
                    0,
                    "note_test",
                    List.of("tag1", "tag2"),
                    null,
                    null,
                    lineItems,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    "VNPAY",
                    "gateWay",
                    new InMemoryIdGenerator(),
                    locationId,
                    List.of()
            );


    @Override
    public void save(Order order) {
    }

    @Override
    public Order findById(OrderId id) {
        return order;
    }
}
