package org.example.order.order.application.model.order.response;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountApplication.RuleType;
import org.example.order.order.domain.order.model.DiscountApplication.TargetType;
import org.example.order.order.domain.order.model.DiscountApplication.ValueType;

import java.math.BigDecimal;

@Getter
@Setter
public class DiscountApplicationResponse {
    private int id;
    private BigDecimal value;
    private ValueType valueType;
    private TargetType targetType;
    private RuleType ruleType;
    private String title;
    private String description;
    private String code;
    private int index;
}
