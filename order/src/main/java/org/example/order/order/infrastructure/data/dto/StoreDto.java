package org.example.order.order.infrastructure.data.dto;

import lombok.*;

import java.util.Currency;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDto {
    private int id;
    private String name;
    private Currency currency;
}
