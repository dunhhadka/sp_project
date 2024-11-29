package org.example.order.order.infrastructure.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.VariantInfo;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class VariantDto {
    private int id;
    private int productId;

    private String title;
    private String sku;
    private String unit;

    private BigDecimal price;
    private int weight;

    private String inventoryManagement;
    //    private String inventoryPolicy;
    private Long inventoryItemId;

    private boolean requiresShipping;
    private boolean taxable;

    private VariantInfo.VariantType type;
}
