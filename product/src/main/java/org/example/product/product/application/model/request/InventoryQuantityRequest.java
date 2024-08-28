package org.example.product.product.application.model.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InventoryQuantityRequest {
    private BigDecimal onHand;
    private long locationId;
}
