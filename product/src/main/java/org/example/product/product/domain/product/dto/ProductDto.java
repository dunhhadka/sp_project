package org.example.product.product.domain.product.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.Product;

import java.time.Instant;

@Getter
@Setter
public class ProductDto {
    private int id;
    private String name;
    private boolean available;

    private String vendor;
    private String productType;
    private String title;
    private String description;
    private String summary;

    private Product.ProductStatus status;

    private Instant createdOn;
    private Instant modifiedOn;
}
