package org.example.order.order.domain.draftorder.model;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Builder
public class DraftDiscountAllocation {
    private @Min(0) BigDecimal amount;
    private @NotNull @Min(0) int discountApplicationIndex;
}
