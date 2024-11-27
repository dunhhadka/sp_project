package org.example.product.product.application.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoredImageResult {
    private String fileName;
    private String contentType;
    private String src;
    private Integer size;
    private Integer width;
    private Integer height;
    private Integer position;
}
