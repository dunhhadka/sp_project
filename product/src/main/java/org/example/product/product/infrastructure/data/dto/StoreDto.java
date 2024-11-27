package org.example.product.product.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreDto {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String alias;
    private int maxProduct;
}
