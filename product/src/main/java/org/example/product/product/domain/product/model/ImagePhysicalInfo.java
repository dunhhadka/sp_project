package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ImagePhysicalInfo {
    private Integer size;
    private Integer width;
    private Integer height;
}
