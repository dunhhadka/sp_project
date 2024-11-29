package org.example.order.order.infrastructure.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductDto {
    private int id;
    private String name;
    private String vendor;
}
