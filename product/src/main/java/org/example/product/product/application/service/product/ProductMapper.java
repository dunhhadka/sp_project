package org.example.product.product.application.service.product;

import org.example.product.product.application.model.response.ProductResponse;
import org.example.product.product.application.model.response.ProductVariantResponse;
import org.example.product.product.domain.product.dto.ProductDto;
import org.example.product.product.domain.product.repository.VariantDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ProductMapper {
    public abstract ProductResponse fromDtoToResponse(ProductDto productDto);

    public abstract ProductVariantResponse fromDtoToResponse(VariantDto variantDto);
}
