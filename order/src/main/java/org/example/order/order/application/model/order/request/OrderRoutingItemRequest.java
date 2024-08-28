package org.example.order.order.application.model.order.request;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderRoutingItemRequest {
    private Integer variantId;
    private BigDecimal quantity;
    private boolean requiresShipping;
}
