package org.example.order.order.domain.draftorder.model;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftAppliedDiscount {
    private String title;
    private String description;
    private BigDecimal value; // chỉ số giảm giá tính trên 1 quantity, nếu type = fixed_amount => amount, percentage => phần trăm

    private ValueType valueType;
    private BigDecimal amount; // tính trên tất cả quantity
    private boolean custom;

    public String getCode() {
        return StringUtils.firstNonBlank(this.title, this.description, "custom discount");
    }

    public enum ValueType {
        fixed_amount, percentage
    }
}
