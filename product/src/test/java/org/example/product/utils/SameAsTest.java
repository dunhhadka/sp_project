package org.example.product.utils;

import org.example.product.product.domain.model.ProductGeneralInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SameAsTest {

    @Test
    public void testSameAs() {
        var generalInfo = ProductGeneralInfo.builder()
                .vendor("abc")
                .productType("normal")
                .build();

        var generalInfo2 = generalInfo.toBuilder().build();

        Assertions.assertNotEquals(generalInfo, generalInfo2);
    }

}
