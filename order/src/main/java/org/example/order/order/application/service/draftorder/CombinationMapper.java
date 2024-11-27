package org.example.order.order.application.service.draftorder;

import org.example.order.order.application.model.draftorder.request.CombinationCalculateRequest;
import org.example.order.order.application.model.draftorder.response.CombinationCalculateResponse;
import org.example.order.order.application.model.draftorder.response.LineItemComponent;
import org.example.order.order.application.model.order.context.ComboItem;
import org.example.order.order.application.model.order.context.Product;
import org.example.order.order.application.model.order.context.ProductVariant;
import org.example.order.order.domain.draftorder.model.VariantType;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class CombinationMapper {

    public abstract ProductVariant toResponse(ComboItem item);

    public CombinationCalculateResponse.LineItem toResponse(CombinationCalculateRequest.LineItem lineItemRequest) {
        return null;
    }

    public CombinationCalculateResponse.LineItem toResponse(CombinationCalculateRequest.LineItem lineItemRequest, ProductVariant variant, Product product) {
        var lineItemResponse = this.toResponse(lineItemRequest);
        if (lineItemResponse != null) {
            if (variant != null) {
                if (lineItemResponse.getVariantTitle() != null) lineItemResponse.setVariantTitle(variant.getTitle());
                if (lineItemResponse.getPrice() == null) lineItemResponse.setPrice(variant.getPrice());
                if (lineItemResponse.getSku() == null) lineItemResponse.setSku(variant.getSku());
            }
            if (product != null && lineItemResponse.getTitle() == null) lineItemResponse.setTitle(product.getName());
        }
        return lineItemResponse;
    }

    public LineItemComponent toResponse(ProductVariant childVariant,
                                        Product childProduct,
                                        BigDecimal quantity,
                                        BigDecimal remainder,
                                        BigDecimal componentPrice,
                                        BigDecimal lineItemPrice,
                                        List<CombinationCalculateRequest.ComboPacksizeDiscountAllocation> discountAllocations,
                                        VariantType variantType) {
        return null;
    }
}

