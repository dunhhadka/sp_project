package org.example.order.application.service.order;

import org.example.order.order.application.service.order.OrderMapper;
import org.example.order.order.application.service.order.OrderWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class OrderWriteServiceTest {

    @TestConfiguration
    @Import({OrderWriteService.class})
    static class Config {
        @Bean
        OrderMapper orderMapper() {
            return Mappers.getMapper(OrderMapper.class);
        }
    }

    @BeforeEach
    void setUp() {

    }

    @Test
    public void test() {

    }
}
