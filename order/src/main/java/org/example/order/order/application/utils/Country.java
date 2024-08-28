package org.example.order.order.application.utils;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Country {
    private int id;
    private String name;
    private String code;
}
