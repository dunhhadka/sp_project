package org.example.order.application.service.orderedit;

import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class,
        classes = {OrderBaseServiceTest.BaseConfiguration.class})
public class OrderBaseServiceTest {


    @Configuration
    static class BaseConfiguration {
        static {
            System.out.printf("configuration base order edit test");
        }
    }
}
