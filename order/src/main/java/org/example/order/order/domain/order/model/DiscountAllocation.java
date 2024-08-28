package org.example.order.order.domain.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Table(name = "discount_allocations")
@NoArgsConstructor
public class DiscountAllocation {
    @Id
    private int id;

    @Min(1)
    private int storeId;

    @Min(1)
    private int orderId;

    @NotNull
    private BigDecimal amount;

    private Integer targetId;

    @Enumerated(value = EnumType.STRING)
    private TargetType targetType;

    private Integer applicationId;

    @NotNull
    private Integer applicationIndex;

    @NotNull
    private Instant createdAt;

    @Version
    private Integer version;

    public DiscountAllocation(
            int id,
            BigDecimal amount,
            int targetId,
            TargetType targetType,
            Integer applicationId,
            int applicationIndex
    ) {
        this.id = id;
        this.amount = amount;
        this.targetId = targetId;
        this.targetType = targetType;
        this.applicationId = applicationId;
        this.applicationIndex = applicationIndex;
    }

    public void setRootId(OrderId orderId) {
        this.storeId = orderId.getStoreId();
        this.orderId = orderId.getId();
    }

    public enum TargetType {
        shipping_line, line_item
    }
}
