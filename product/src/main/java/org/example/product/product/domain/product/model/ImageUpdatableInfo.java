package org.example.product.product.domain.product.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUpdatableInfo {
    private String alt;
    private String src;
    private String fileName;
    private ImagePhysicalInfo physicalInfo;
    private Integer position;
}
