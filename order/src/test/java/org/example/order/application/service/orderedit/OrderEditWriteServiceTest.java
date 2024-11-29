package org.example.order.application.service.orderedit;

import lombok.extern.slf4j.Slf4j;
import org.example.order.SapoClient;
import org.example.order.order.application.service.orderedit.OrderEditRequest;
import org.example.order.order.application.service.orderedit.OrderEditWriteService;
import org.example.order.order.application.utils.TaxHelper;
import org.example.order.order.application.utils.TaxSetting;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.orderedit.persistence.OrderEditRepository;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dto.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
public class OrderEditWriteServiceTest extends OrderBaseServiceTest {

    @Autowired
    OrderEditWriteService orderEditWriteService;

    @MockBean
    ProductDao productDao;

    @MockBean
    TaxHelper taxHelper;

    @MockBean
    SapoClient sapoClient;

    @Autowired
    OrderEditRepository orderEditRepository;

    static class Fixtures {
        public static final OrderId orderId = new OrderId(1, 1);

        public static final Location location = Location.builder().id(1).name("location1").build();

        public static final TaxSetting taxSetting = TaxSetting.builder().taxes(List.of()).build();
    }

    @BeforeEach
    void setUp() {
        when(sapoClient.locationList(any())).thenReturn(List.of(Fixtures.location));
        when(taxHelper.getTaxSetting(anyInt(), any(), any())).thenReturn(Fixtures.taxSetting);
    }

    @AfterEach
    void reset() {
        Mockito.reset(sapoClient, taxHelper, productDao);
    }

    @Test
    public void save_order_edit_then_add_variants() {
        when(productDao.findVariantByListIds(anyInt(), anyList())).thenReturn(ProductTestUtils.variants);
        when(productDao.findProductByListIds(anyInt(), anyList())).thenReturn(ProductTestUtils.products);

        var orderEditId = orderEditWriteService.beginEdit(Fixtures.orderId);

        var addVariantsRequest = createAddVariantRequest();

        orderEditWriteService.addVariants(orderEditId, addVariantsRequest);

        var orderEdit = orderEditRepository.findById(orderEditId);

        Assertions.assertEquals(2, orderEdit.getLineItems().size());
        Assertions.assertEquals(BigDecimal.valueOf(30 + 2 + 3), orderEdit.getSubtotalLineItemQuantity().stripTrailingZeros());
    }

    private OrderEditRequest.AddVariants createAddVariantRequest() {
        OrderEditRequest.AddVariant addVariant1 = OrderEditRequest.AddVariant.builder()
                .variantId(5)
                .quantity(BigDecimal.valueOf(2))
                .locationId(1)
                .allowDuplicate(true)
                .build();
        OrderEditRequest.AddVariant addVariant2 = OrderEditRequest.AddVariant.builder()
                .variantId(6)
                .quantity(BigDecimal.valueOf(3))
                .locationId(1)
                .allowDuplicate(true)
                .build();
        return OrderEditRequest.AddVariants.builder()
                .addVariants(List.of(addVariant1, addVariant2))
                .build();
    }
}
