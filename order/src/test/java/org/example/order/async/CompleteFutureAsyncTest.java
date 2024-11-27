package org.example.order.async;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

public class CompleteFutureAsyncTest {

    @Test
    public void test() {
        var taxSetting = CompletableFuture.supplyAsync(() -> "taxSetting");
        var productTaxes = CompletableFuture.supplyAsync(() -> "productTaxes");
        var shippingTaxes = CompletableFuture.supplyAsync(() -> "shippingTaxes");

        CompletableFuture.allOf(taxSetting, productTaxes, shippingTaxes)
                .join();

        var tax = taxSetting.join();
        var product = productTaxes.join();
        var shipping = shippingTaxes.join();

        Assertions.assertEquals("taxSetting", tax);
        Assertions.assertEquals("productTaxes", product);
        Assertions.assertEquals("shippingTaxes", shipping);
    }
}
