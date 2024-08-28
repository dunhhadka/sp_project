package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Currency;

@Getter
@Setter
public class StoreDto {
    private int id;
    private String name;
    private Currency currency;
}