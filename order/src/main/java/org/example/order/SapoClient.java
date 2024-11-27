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

import java.util.List;

public interface SapoClient {

    List<InventoryLevel> inventoryLevels(InventoryClientService.InventoryLevelFilterRequest inventoryLevelFilter);

    List<Location> locationList(LocationFilter locationFilter);

    Location location(LocationFilter locationFilter);

    List<Combo> comboFilter(int storeId, List<Integer> comboIds);

    List<ProductVariant> productVariantFilter(int storeId, List<Integer> variantIds);

    List<Packsize> packsizeFilterByVariantIds(int storeId, List<Integer> packsizeIds);

    List<Product> productFilter(Integer storeId, List<Integer> allProductIds);

    TaxSettingResponse taxesSetting(int storeId);

    List<ProductTax> productTaxList(ProductTaxFilter productTaxFilter);

    ShippingTax shippingTaxGet(ShippingTaxFilter shippingTaxFilter);
}
