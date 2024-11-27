package org.example.order.order.application.model.order.context;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class Combo {
    private int variantId;
    private int productId;
    private String unit;
    private String itemUnit;
    private BigDecimal price;
    private List<ComboItem> comboItems;
}
