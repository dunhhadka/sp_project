package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProductGeneralInfo {
    @Size(max = 320)
    private String metaTitle;
    @Size(max = 320)
    private String metaDescription;
    @Size(max = 255)
    private String templateLayout;
    @Size(max = 1000)
    private String summary;
    @Size(max = 255)
    private String vendor;
    @Size(max = 255)
    private String productType;
}
