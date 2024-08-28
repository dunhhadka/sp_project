package org.example.order.order.domain.order.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "tax_lines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxLine {
    @Id
    private int id;

    @Positive
    private int storeId;

    @Positive
    private int orderId;

    private String title;

    @NotNull
    @PositiveOrZero
    private BigDecimal rate;

    @NotNull
    @PositiveOrZero
    private BigDecimal price = BigDecimal.ZERO;

    private Integer targetId;

    @Enumerated(value = EnumType.STRING)
    private TargetType targetType;

    private Integer quantity;

    private boolean custom;

    @Version
    private Integer version;

    public TaxLine(
            int id,
            BigDecimal rate,
            String title,
            BigDecimal price,
            int targetId,
            TargetType targetType,
            int quantity
    ) {
        this.id = id;
        this.rate = rate;
        this.title = title;
        this.price = price;
        this.targetId = targetId;
        this.targetType = targetType;
        this.quantity = quantity;
    }

    public void setRootId(OrderId orderId) {
        this.orderId = orderId.getId();
        this.storeId = orderId.getStoreId();
    }

    public enum TargetType {
        shipping_line,
        line_item
    }
}
