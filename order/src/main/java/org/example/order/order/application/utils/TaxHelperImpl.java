package org.example.order.order.application.utils;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.SapoClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class TaxHelperImpl implements TaxHelper {

    private final SapoClient sapoClient;

    @Override
    public TaxSetting getTaxSetting(int storeId, String countryCode, Set<Integer> productIds) {
        return this.getTaxSetting(storeId, countryCode, productIds, false);
    }

    private TaxSetting getTaxSetting(int storeId, String countryCode, Set<Integer> productIds, boolean includeShipping) {
        var taxSettingSupply = CompletableFuture.supplyAsync(() -> handleSupplier(() ->
                sapoClient.taxesSetting(storeId).getTaxSetting()
        ));

        CompletableFuture<List<ProductTax>> productTaxSupply = null;
        if (CollectionUtils.isEmpty(productIds)) {
            productTaxSupply = CompletableFuture.completedFuture(List.of());
        } else {
            productTaxSupply = CompletableFuture.supplyAsync(() -> handleSupplier(() ->
                    sapoClient.productTaxList(ProductTaxFilter.builder()
                            .countryCode(countryCode)
                            .productIds(productIds)
                            .build())
            ));
        }

        CompletableFuture<ShippingTax> shippingTaxSupply;
        if (!includeShipping) {
            shippingTaxSupply = CompletableFuture.completedFuture(null);
        } else {
            shippingTaxSupply = CompletableFuture.supplyAsync(() -> handleSupplier(() ->
                    sapoClient.shippingTaxGet(ShippingTaxFilter.builder()
                            .countryCode(countryCode)
                            .build())
            ));
        }

        CompletableFuture.allOf(taxSettingSupply, productTaxSupply, shippingTaxSupply)
                .join();

        var taxSetting = taxSettingSupply.join();
        var productsTax = productTaxSupply.join();
        var shippingTax = shippingTaxSupply.join();

        if (taxSetting != null && productsTax != null && (!includeShipping || shippingTax != null)) {
            var taxes = new ArrayList<TaxSettingValue>();
            for (var productTax : productsTax) {
                var taxBuilder = TaxSettingValue.builder()
                        .rate(productTax.getTaxRate())
                        .title(productTax.getTaxName());
                if (productTax.getProductId() > 0) {
                    taxBuilder.productId(productTax.getProductId())
                            .taxType(TaxSettingValue.TaxType.line_item);
                }
                taxes.add(taxBuilder.build());
            }
            if (shippingTax != null) {
                var taxShippingSetting = TaxSettingValue.builder()
                        .rate(shippingTax.getTaxRate())
                        .title(shippingTax.getTaxName())
                        .taxType(TaxSettingValue.TaxType.shipping)
                        .build();
                taxes.add(taxShippingSetting);
            }
            return TaxSetting.builder()
                    .taxIncluded(taxSetting.isTaxIncluded())
                    .taxShipping(includeShipping)
                    .taxes(taxes)
                    .productIds(productIds)
                    .countryCode(countryCode)
                    .build();
        }

        return TaxSetting.defaultTax();
    }

    private <T> T handleSupplier(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw e;
        }
    }

}
