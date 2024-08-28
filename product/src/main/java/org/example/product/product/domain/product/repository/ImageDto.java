package org.example.product.product.domain.product.repository;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ImageDto {
    private int id;
    private int productId;
    private int storeId;
    private int position;
    private Instant createdOn;
    private Instant modifiedOn;
    private String alt;
    private String src;
    private String filename;
    private Integer size;
    private Integer width;
    private Integer height;
}
