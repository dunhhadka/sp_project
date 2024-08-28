package org.example.product.product.application.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoredImageResult {
    private String src;
    private String fileName;
    private Integer size;
    private Integer width;
    private Integer height;
}
