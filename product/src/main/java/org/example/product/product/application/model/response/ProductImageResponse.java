package org.example.product.product.application.model.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ProductImageResponse {
    private int id;
    private int productId;
    private List<Integer> variantIds;
    private Instant createdOn;
    private Instant modifiedOn;
    private String src;
    private String alt;
    private String fileName;
    private Integer size;
    private Integer width;
    private Instant height;
}
