package org.example.order;

import org.example.order.order.application.model.fulfillmentorder.request.InventoryLevel;
import org.example.order.order.application.model.order.context.Combo;
import org.example.order.order.application.model.order.context.Packsize;
import org.example.order.order.application.model.order.context.Product;
import org.example.order.order.application.model.order.context.ProductVariant;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.application.service.fulfillmentorder.InventoryClientService;
import org.example.order.order.application.utils.*;
import org.example.order.order.infrastructure.data.dto.Location;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SapoClientImpl implements SapoClient {
    @Override
    public List<InventoryLevel> inventoryLevels(InventoryClientService.InventoryLevelFilterRequest inventoryLevelFilter) {
        return null;
    }

    @Override
    public List<Location> locationList(LocationFilter locationFilter) {
        return null;
    }

    @Override
    public Location location(LocationFilter locationFilter) {
        return null;
    }

    @Override
    public List<Combo> comboFilter(int storeId, List<Integer> comboIds) {
        return List.of();
    }

    @Override
    public List<ProductVariant> productVariantFilter(int storeId, List<Integer> variantIds) {
        return List.of();
    }

    @Override
    public List<Packsize> packsizeFilterByVariantIds(int storeId, List<Integer> packsizeIds) {
        return List.of();
    }

    @Override
    public List<Product> productFilter(Integer storeId, List<Integer> allProductIds) {
        return List.of();
    }

    @Override
    public TaxSettingResponse taxesSetting(int storeId) {
        return new TaxSettingResponse(
                TaxSettingResponse.TaxSetting.builder()
                        .build()
        );
    }

    @Override
    public List<ProductTax> productTaxList(ProductTaxFilter productTaxFilter) {
        return List.of();
    }

    @Override
    public ShippingTax shippingTaxGet(ShippingTaxFilter shippingTaxFilter) {
        return new ShippingTax();
    }
}
