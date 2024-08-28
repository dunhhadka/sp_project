package org.example.order.order.application.model.order.context;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Product {
    private int id;
    private String name;
    private List<ProductVariant> variants;
    private List<ProductImage> images;
}
