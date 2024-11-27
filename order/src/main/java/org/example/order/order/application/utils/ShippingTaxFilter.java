package org.example.order.order.application.utils;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShippingTaxFilter {
    private String countryCode;
}
