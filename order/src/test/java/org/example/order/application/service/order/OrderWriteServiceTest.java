package org.example.order.application.service.order;

import lombok.extern.slf4j.Slf4j;
import org.example.order.infrastructure.persistence.InMemoryIdGenerator;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.service.customer.CustomerService;
import org.example.order.order.application.service.order.OrderMapper;
import org.example.order.order.application.service.order.OrderMapperImpl;
import org.example.order.order.application.service.order.OrderWriteService;
import org.example.order.order.application.service.order.RefundCalculationService;
import org.example.order.order.domain.order.model.OrderIdGenerator;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.transaction.model.TransactionRepository;
import org.example.order.order.infrastructure.data.dao.LocationDao;
import org.example.order.order.infrastructure.data.dao.OrderDao;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dao.StoreDao;
import org.example.order.order.infrastructure.data.dto.StoreDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@Slf4j
@SpringJUnitConfig
public class OrderWriteServiceTest {

    @TestConfiguration
    @Import({OrderWriteService.class})
    static class Config {
        @Bean
        OrderIdGenerator idGenerator() {
            return new InMemoryIdGenerator();
        }

        @Bean
        OrderMapper orderMapper() {
            return new OrderMapperImpl();
        }

        @Bean
        public int haDung() {
            log.info("bean was created");
            return 1234;
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OrderWriteService orderWriteService;

    @MockBean
    private StoreDao storeDao;
    @MockBean
    private OrderDao orderDao;
    @MockBean
    private ProductDao productDao;
    @MockBean
    private LocationDao locationDao;
    @MockBean
    private OrderMapper orderMapper;
    @MockBean
    private OrderRepository orderRepository;
    @MockBean
    private TransactionRepository transactionRepository;
    @MockBean
    private CustomerService customerService;
    @MockBean
    private RefundCalculationService calculationService;

    @BeforeEach
    void beforeEach() {
        var store = StoreDto.builder()
                .id(1)
                .name("store")
                .currency(Currency.getInstance("VND"))
                .build();
        when(storeDao.findById(anyInt())).thenReturn(store);
    }

    @Test
    void createOrderWithCustomProductInfo() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (var bean : beanNames) {
            log.info(bean);
        }
        var orderCreatedRequest = OrderCreateRequest.builder()
                .lineItems(
                        List.of(
                                OrderCreateRequest.LineItemRequest.builder()
                                        .price(new BigDecimal(1000))
                                        .title("A")
                                        .vendor("a")
                                        .quantity(2)
                                        .build(),
                                OrderCreateRequest.LineItemRequest.builder()
                                        .price(new BigDecimal(1000))
                                        .title("B")
                                        .vendor("b")
                                        .quantity(3)
                                        .build()

                        )
                )
                .build();
        orderWriteService.create(1, orderCreatedRequest);
    }
}
