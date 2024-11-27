package org.example.order.order.application.model.draftorder.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.draftorder.model.DraftAppliedDiscount;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DraftAppliedDiscountRequest {
    @Size(max = 255)
    private String title;
    @Size(max = 250)
    private String description;
    private BigDecimal value;
    @NotNull
    private DraftAppliedDiscount.ValueType valueType;

    private BigDecimal amount;
    @Builder.Default
    private boolean custom = true;
}
