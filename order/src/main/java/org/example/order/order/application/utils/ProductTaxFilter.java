package org.example.order.order.application.utils;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class ProductTaxFilter {
    private String countryCode;
    private Set<Integer> productIds;
}
