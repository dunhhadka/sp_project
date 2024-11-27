package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderEditDiscountApplicationDto {
    private UUID id;
    private int storeId;
    private int editingId;

    private String code;
    private String description;

    private BigDecimal value;
    private BigDecimal maxValue;
    private DiscountApplication.ValueType valueType;

    private DiscountApplication.RuleType ruleType;

    private Instant updatedAt;
    private Integer version;
}
