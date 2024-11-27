package org.example.order.order.application.model.orderedit;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.infrastructure.data.dto.OrderEditDiscountApplicationDto;

import java.math.BigDecimal;

@Getter
@Setter
public class CalculatedDiscountApplication {
    private int id;
    private String code;
    private String description;
    private BigDecimal value;
    private BigDecimal maxValue;
    private DiscountApplication.ValueType valueType;

    private DiscountApplication.RuleType ruleType;

    public CalculatedDiscountApplication(OrderEditDiscountApplicationDto discount) {

    }
}
