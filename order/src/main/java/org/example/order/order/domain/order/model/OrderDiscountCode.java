package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.order.application.utils.BigDecimals;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "order_discounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderDiscountCode {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    private Order aggRoot;

    @Id
    private int id;

    @Size(max = 255)
    private String code;

    @Setter
    @NotNull
    @Min(0)
    private BigDecimal amount;

    @Enumerated(value = EnumType.STRING)
    private ValueType type;

    private Boolean custom;

    @Transient
    private BigDecimal value;

    public OrderDiscountCode(
            int id,
            String code,
            ValueType valueType,
            BigDecimal amount,
            boolean custom,
            boolean isPrecalculateAmount
    ) {
        this.id = id;
        this.code = code;
        this.type = valueType;
        this.custom = custom;
        this.amount = amount;
        this.value = amount;
        if (isPrecalculateAmount) {
            this.amount = amount;
        } else {
            if (this.type == ValueType.percentage) {
                this.value = BigDecimals.ONE_HUNDRED.min(this.value);
            }
        }
    }

    public enum ValueType {
        fixed_amount,
        percentage,
        shipping_line
    }
}
