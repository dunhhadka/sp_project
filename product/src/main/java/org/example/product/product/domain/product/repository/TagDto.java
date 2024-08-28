package org.example.product.product.domain.product.repository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagDto {
    private int storeId;
    private int productId;
    private String name;
}
