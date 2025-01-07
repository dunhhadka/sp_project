package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.OrderStagedChangeModel;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;

public final class OrderEditMapper {

    public static OrderStagedChangeModel map(OrderStagedChange.AddVariant addVariant) {
        var model = new OrderStagedChangeModel.AddedVariant();
        model.setLineItemId(addVariant.getLineItemId());
        model.setVariantId(addVariant.getVariantId());
        model.setQuantity(addVariant.getQuantity());
        model.setLocationId(addVariant.getLocationId());
        return model;
    }

    public static OrderStagedChangeModel map(OrderStagedChange.AddCustomItem addCustomItem) {
        var model = new OrderStagedChangeModel.AddCustomItem();
        model.setLineItemId(addCustomItem.getLineItemId());
        model.setTitle(addCustomItem.getTitle());
        model.setPrice(addCustomItem.getPrice());
        model.setQuantity(addCustomItem.getQuantity());
        model.setRequireShipping(addCustomItem.isRequireShipping());
        model.setTaxable(addCustomItem.isTaxable());
        model.setLocationId(addCustomItem.getLocationId());
        return model;
    }

    public static OrderStagedChangeModel map(OrderStagedChange.AddItemDiscount addItemDiscount) {
        var model = new OrderStagedChangeModel.AddItemDiscount();
        model.setLineItemId(addItemDiscount.getLineItemId());
        model.setApplicationId(addItemDiscount.getApplicationId());
        model.setDescription(addItemDiscount.getDescription());
        model.setValue(addItemDiscount.getValue());
        return model;
    }
}
