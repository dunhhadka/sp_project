package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.TaxLine;

import java.math.BigDecimal;

@Getter
@Setter
public class TaxLineDto {
    private int id;

    private int storeId;

    private int orderId;

    private String title;

    private BigDecimal rate;

    private BigDecimal price = BigDecimal.ZERO;

    private Integer targetId;

    private TaxLine.TargetType targetType;

    private Integer quantity;

    private boolean custom;

    private Integer version;
}
