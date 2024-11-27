package org.example.product.product.application.model;

import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.Product;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

@Setter
@Getter
public class ProductRequest {
    private @Size(max = 320) String name;
    private @Size(max = 150) String alias;
    private @Size(max = 255) String vendor;
    private @Size(max = 255) String productType;
    private @Size(max = 320) String metaTitle;
    private @Size(max = 320) String metaDescription;
    private @Size(max = 1000) String summary;
    private Instant publishedOn;
    private @Size(max = 255) String templateLayout;
    private String tags;
    private @Size(max = 255) String defaultVariantUnit;
    private @Size(max = 250) List<@Valid ProductImageRequest> images;
    private @Size(max = 100) List<@Valid ProductVariantRequest> variants;
    private Product.ProductStatus status;
}
