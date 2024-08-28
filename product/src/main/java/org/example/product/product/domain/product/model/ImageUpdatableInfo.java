package org.example.product.product.domain.product.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ImageUpdatableInfo {
    private String alt;
    private String src;
    private String fileName;
    private Integer position;
    private ImagePhysicalInfo physicalInfo;
}
