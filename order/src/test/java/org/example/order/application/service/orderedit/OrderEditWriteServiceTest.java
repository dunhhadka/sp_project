package org.example.order.application.service.orderedit;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
public class OrderEditWriteServiceTest extends OrderBaseServiceTest {

    @Test
    public void test() {
        Assertions.assertEquals(1, 1);
    }
}
