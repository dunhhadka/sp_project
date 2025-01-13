package org.example.order.order.application.service.draftorder;

import org.example.order.order.application.model.draftorder.request.CombinationCalculateRequest;
import org.example.order.order.application.model.order.context.ComboItem;
import org.example.order.order.application.model.order.context.Product;
import org.example.order.order.application.model.order.context.ProductVariant;
import org.example.order.order.domain.draftorder.model.VariantType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class CombinationMapper {


    public abstract ProductVariant toResponse(ComboItem item);

    public abstract CombinationCalculateResponse.LineItem toResponse(CombinationCalculateRequest.LineItem lineItemRequest);

    public CombinationCalculateResponse.LineItem toResponse(CombinationCalculateRequest.LineItem lineItemRequest, ProductVariant variant, Product product) {
        var lineItemResponse = this.toLineItemResponse(lineItemRequest, variant, product);
        if (lineItemResponse != null) {
            if (variant != null) {
                if (lineItemResponse.getVariantTitle() == null) lineItemResponse.setVariantTitle(variant.getTitle());
                if (lineItemResponse.getPrice() == null) lineItemResponse.setPrice(variant.getPrice());
                if (lineItemResponse.getSku() == null) lineItemResponse.setSku(variant.getSku());
            }
            if (product != null && lineItemResponse.getTitle() == null) {
                lineItemResponse.setTitle(product.getName());
            }
        }
        return lineItemResponse;
    }

    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "productId", source = "variant.productId")
    @Mapping(target = "title", source = "lineItem.title")
    @Mapping(target = "variantTitle", source = "lineItem.variantTitle")
    @Mapping(target = "quantity", source = "lineItem.quantity")
    @Mapping(target = "price", source = "lineItem.price")
    @Mapping(target = "linePrice", source = "lineItem.linePrice")
    @Mapping(target = "unit", source = "variant.unit")
    @Mapping(target = "type", source = "variant.type")
    @Mapping(target = "taxable", source = "variant.taxable")
    @Mapping(target = "requiresShipping", source = "variant.requiresShipping")
    @Mapping(target = "grams", source = "variant.grams")
    @Mapping(target = "sku", source = "lineItem.sku")
    protected abstract CombinationCalculateResponse.LineItem toLineItemResponse(CombinationCalculateRequest.LineItem lineItemRequest, ProductVariant variant, Product product);


    @Mapping(target = "variantId", source = "childVariant.id")
    @Mapping(target = "productId", source = "childProduct.id")
    @Mapping(target = "sku", source = "childVariant.sku")
    @Mapping(target = "variantTitle", source = "childVariant.title")
    @Mapping(target = "title", source = "childProduct.name")
    @Mapping(target = "grams", source = "childVariant.grams")
    @Mapping(target = "taxable", source = "childVariant.taxable")
    @Mapping(target = "linePrice", source = "linePrice")
    @Mapping(target = "price", source = "componentPrice")
    @Mapping(target = "vendor", source = "childProduct.vendor")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "source", source = "source")
    @Mapping(target = "requiresShipping", source = "childVariant.requiresShipping")
    @Mapping(target = "unit", source = "childVariant.unit")
    @Mapping(target = "remainder", source = "remainder")
    @Mapping(target = "inventoryItemId", source = "childVariant.inventoryItemId")
    @Mapping(target = "inventoryManagement", source = "childVariant.inventoryManagement")
    @Mapping(target = "inventoryPolicy", source = "childVariant.inventoryPolicy")
    public abstract CombinationCalculateResponse.LineItemComponent toResponse(ProductVariant childVariant,
                                                                              Product childProduct,
                                                                              BigDecimal quantity,
                                                                              BigDecimal baseQuantity,
                                                                              BigDecimal remainder,
                                                                              BigDecimal componentPrice,
                                                                              BigDecimal lineItemPrice,
                                                                              List<CombinationCalculateResponse.ComboPacksizeDiscountAllocation> discountAllocations,
                                                                              VariantType variantType);

}

