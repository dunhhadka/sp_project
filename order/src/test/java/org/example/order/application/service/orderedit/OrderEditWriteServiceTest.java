package org.example.order.application.service.orderedit;

import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.service.orderedit.OrderEditContextService;
import org.example.order.order.application.service.orderedit.OrderEditRequest;
import org.example.order.order.application.service.orderedit.OrderEditWriteService;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.infrastructure.persistence.JpaOrderEditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;

@Slf4j
public class OrderEditWriteServiceTest extends OrderBaseServiceTest {

    @Autowired
    ApplicationContext applicationContext;

    @MockBean
    OrderRepository orderRepository;

    @Autowired
    JpaOrderEditRepository orderEditRepository;

    @Autowired
    OrderEditContextService orderEditContextService;

    @Autowired
    OrderEditWriteService orderEditWriteService;

    static class Fixtures {
        public static final OrderId orderId = new OrderId(1, 1);
    }

    @BeforeEach
    void setUp() {
        when(orderRepository.findById(Fixtures.orderId)).thenReturn(TestOrderRepository.order);
    }

    @Test
    public void test() {
        var orderEditId = orderEditWriteService.beginEdit(Fixtures.orderId);

        var addVariants = getVariants();

        orderEditWriteService.addVariants(orderEditId, addVariants);

        var orderEdit = orderEditRepository.findById(orderEditId);
    }

    private OrderEditRequest.AddVariants getVariants() {
        var addVariants = new OrderEditRequest.AddVariants();
        var addVariant1 = OrderEditRequest.AddVariant.builder()
                .variantId(1)
                .quantity(BigDecimal.valueOf(5))
                .locationId(5)
                .allowDuplicate(true)
                .build();

        addVariants.setAddVariants(List.of(addVariant1));

        return addVariants;
    }
}
