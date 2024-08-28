package org.example.product.product.application.model.response;

import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.Product;

import java.util.List;

@Getter
@Setter
public class ProductResponse {
    private int id;
    private String name;
    private boolean available;

    private String vendor;
    private String productType;
    private String title;
    private String description;
    private String summary;

    private String tags;

    private Product.ProductStatus status;

    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
}
