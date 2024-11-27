package org.example.order.order.application.model.order.context;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class Product {
    private int id;
    private String name;
    private String alias;
    private String vendor;
    private String productType;
    private String metaTitle;
    private String metaDescription;
    private String summary;
    private String templateLayout;
    private Instant createdOn;
    private Instant modifiedOn;
    private Instant publishedOn;
    private String content;
    private String tags;
    private List<ProductVariant> variants;
    private List<ProductImage> images;
}
