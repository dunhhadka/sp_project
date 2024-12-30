package org.example.order.application.service.fulfillmentorder;

import lombok.extern.slf4j.Slf4j;
import org.example.order.infrastructure.persistence.InMemoryIdGenerator;
import org.example.order.order.application.service.fulfillment.FulfillmentOrderWriteService;
import org.example.order.order.application.service.fulfillmentorder.FulfillmentOrderHelperService;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;

@Slf4j
class FulfillmentOrderWriteServiceTest implements FulfillmentOrderFixtures {

    InMemoryIdGenerator idGenerator;

    @Mock
    OrderRepository orderRepository;
    @Mock
    FulfillmentOrderRepository fulfillmentOrderRepository;
    @Mock
    FulfillmentOrderHelperService fulfillmentOrderHelperService;

    @MockBean
    ApplicationEventPublisher applicationEventPublisher;

    @MockBean
    MessageSource messageSource;

    @InjectMocks
    FulfillmentOrderWriteService fulfillmentOrderWriteService;

    @BeforeEach
    void setUp() {
        idGenerator = new InMemoryIdGenerator();
        fulfillmentOrderWriteService = new FulfillmentOrderWriteService(
                orderRepository,
                fulfillmentOrderRepository,
                fulfillmentOrderHelperService,
                applicationEventPublisher
        );
    }

    @Test
    public void test() {

    }
}