package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import org.example.order.order.domain.order.model.DiscountApplication;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Getter
@Builder
public class DraftDiscountApplication {
    @Min(0)
    private int index;
    @Size(max = 255)
    private String code;
    @Size(max = 255)
    private String title;
    @Size(max = 250)
    private String description;

    @NotNull
    private BigDecimal value;
    private BigDecimal maxValue;
    private BigDecimal amount;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private DiscountApplication.ValueType type;

    @Enumerated(value = EnumType.STRING)
    private DiscountApplication.TargetType targetType;
}
