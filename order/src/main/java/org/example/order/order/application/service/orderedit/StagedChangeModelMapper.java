package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.OrderStagedChangeModel;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.OrderStagedChangeDto;

public final class StagedChangeModelMapper {
    public static OrderStagedChangeModel toModel(OrderStagedChange.AddVariant addVariant) {
        var model = new OrderStagedChangeModel.AddedVariant();
        model.setLineItemId(addVariant.getLineItemId());
        model.setVariantId(addVariant.getVariantId());
        model.setQuantity(addVariant.getQuantity());
        model.setLocationId(addVariant.getLocationId());
        return model;
    }

    public static OrderStagedChangeModel toModel(OrderStagedChange.AddCustomItem addCustomItem) {
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

    public static OrderStagedChangeModel toModel(OrderStagedChange.AddItemDiscount addItemDiscount) {
        var model = new OrderStagedChangeModel.AddItemDiscount();
        model.setLineItemId(addItemDiscount.getLineItemId());
        model.setApplicationId(addItemDiscount.getApplicationId());
        model.setDescription(addItemDiscount.getDescription());
        model.setValue(addItemDiscount.getValue());
        return model;
    }

    public static OrderStagedChangeModel toModel(OrderStagedChange.IncrementItem incrementItem) {
        var model = new OrderStagedChangeModel.IncrementItem();
        model.setLineItemId(incrementItem.getLineItemId());
        model.setDelta(incrementItem.getDelta());
        model.setLocationId(incrementItem.getLocationId());
        return model;
    }

    public static OrderStagedChangeModel toModel(OrderStagedChange.DecrementItem decrementItem) {
        var model = new OrderStagedChangeModel.DecrementItem();
        model.setLineItemId(decrementItem.getLineItemId());
        model.setDelta(decrementItem.getDelta());
        model.setLocationId(decrementItem.getLocationId());
        return model;
    }

    public static OrderStagedChangeModel toModel(OrderStagedChangeDto change) {
        var stagedChange = OrderEditUtils.convert(change);
        var action = stagedChange.getAction();
        if (action instanceof OrderStagedChange.AddVariant addVariant) return toModel(addVariant);
        else if (action instanceof OrderStagedChange.AddCustomItem addCustomItem) return toModel(addCustomItem);
        else if (action instanceof OrderStagedChange.DecrementItem decrementItem) return toModel(decrementItem);
        else if (action instanceof OrderStagedChange.IncrementItem incrementItem) return toModel(incrementItem);
        else if (action instanceof OrderStagedChange.AddItemDiscount addItemDiscount) return toModel(addItemDiscount);
        else {
            throw new IllegalArgumentException("unsupported");
        }
    }
}
