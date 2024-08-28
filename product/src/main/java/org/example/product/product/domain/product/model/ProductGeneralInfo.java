package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ProductGeneralInfo {
    @Size(max = 255)
    private String vendor;
    @Size(max = 255)
    private String productType;
    @Size(max = 320)
    private String title;
    @Size(max = 320)
    private String description;
    @Size(max = 1000)
    private String summary;
}
