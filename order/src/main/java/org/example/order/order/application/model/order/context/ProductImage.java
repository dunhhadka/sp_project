package org.example.order.order.application.model.order.context;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductImage {
    private int id;
    private List<Integer> variantIds;
    private String src;
}
