package org.example.order.order.application.model.orderedit;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.infrastructure.data.dto.DiscountAllocationDto;
import org.example.order.order.infrastructure.data.dto.OrderEditDiscountAllocationDto;

import java.math.BigDecimal;

@Getter
@Setter
public class CalculatedDiscountAllocation {
    private BigDecimal allocateAmount;
    private Integer discountApplicationId;

    public CalculatedDiscountAllocation(OrderEditDiscountAllocationDto allocation) {
        this.allocateAmount = allocation.getAllocatedAmount();
        this.discountApplicationId = allocation.getApplicationId();
    }

    public CalculatedDiscountAllocation(DiscountAllocationDto discount) {
        this.allocateAmount = discount.getAmount();
        this.discountApplicationId = discount.getApplicationId();
    }
}
