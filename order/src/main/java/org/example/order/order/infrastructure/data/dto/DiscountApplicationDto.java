package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountApplication;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class DiscountApplicationDto {
    private int storeId;
    private int orderId;
    private int id;

    private BigDecimal value;

    private Integer applyIndex;
    private DiscountApplication.ValueType valueType;
    private DiscountApplication.TargetType targetType;

    private Instant createdAt;

    private Integer version;

    private String code;

    private String title;

    private String description;

    private DiscountApplication.RuleType ruleType;
}
