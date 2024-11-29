package org.example.order.application.service.orderedit;

import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;

import java.math.BigDecimal;
import java.util.List;

public class ProductTestUtils {

    public static List<VariantDto> variants = getVariants();
    public static List<ProductDto> products = getProducts();

    private static List<ProductDto> getProducts() {
        ProductDto product5 = ProductDto.builder()
                .id(5)
                .name("sản phẩm 5")
                .build();
        ProductDto product6 = ProductDto.builder()
                .id(6)
                .name("sản phẩm 6")
                .build();
        return List.of(product5, product6);
    }

    private static List<VariantDto> getVariants() {
        VariantDto variant5 = VariantDto.builder()
                .id(5)
                .productId(5)
                .price(BigDecimal.valueOf(150_000))
                .title("variant_with_id_5")
                .unit("unit_variant_5")
                .sku("sku")
                .build();
        VariantDto variant6 = VariantDto.builder()
                .id(6)
                .productId(6)
                .price(BigDecimal.valueOf(110_000))
                .title("variant_with_id_6")
                .unit("unit_variant_6")
                .sku("sku")
                .build();

        return List.of(variant5, variant6);
    }
}
