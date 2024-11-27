package org.example.product.product.domain.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagePhysicalInfo {
    private Integer size;
    private Integer width;
    private Integer height;
}
