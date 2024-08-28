package org.example.product.product.application.model.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.Product;

import java.util.List;

@Getter
@Setter
public class ProductRequest {

    private @Size(max = 320) String name;
    private @Size(max = 255) String vendor;
    private @Size(max = 255) String productType;
    private @Size(max = 320) String title;
    private @Size(max = 320) String description;
    private @Size(max = 1000) String summary;
    private String tags;
    private String defaultVariantUnit;
    private @Size(max = 250) List<ProductImageRequest> images;
    private @Size(max = 100) List<ProductVariantRequest> variants;
    private Product.ProductStatus status;
}
