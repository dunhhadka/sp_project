package org.example.order.application.service.orderedit;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

@Slf4j
public class OrderEditWriteServiceTest extends OrderBaseServiceTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void test() {
        Arrays.stream(applicationContext.getBeanDefinitionNames())
                .forEach(log::info);
    }
}
