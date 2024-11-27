package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "discount_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountApplication {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order aggRoot;

    @Id
    private int id;

    @NotNull
    private BigDecimal value;

    private Integer applyIndex;

    @Enumerated(value = EnumType.STRING)
    private ValueType valueType;

    @Enumerated(value = EnumType.STRING)
    private TargetType targetType;

    @NotNull
    private Instant createdAt;

    @Version
    private Integer version;

    @Size(max = 255)
    private String code;

    @Size(max = 250)
    private String title;

    @Size(max = 250)
    private String description;

    @Enumerated(value = EnumType.STRING)
    private RuleType ruleType;

    public DiscountApplication(
            int id,
            int applyIndex,
            BigDecimal value,
            ValueType valueType,
            TargetType targetType,
            RuleType ruleType
    ) {
        this.id = id;
        this.applyIndex = applyIndex;
        this.value = value;
        this.valueType = valueType;
        this.targetType = targetType;
        this.ruleType = ruleType;

        this.createdAt = Instant.now();
    }

    public DiscountApplication setDiscountNames(String code, String title, String description) {
        this.code = code;
        this.title = title;
        this.description = description;
        if (this.description == null) {
            if (this.title != null) this.description = this.title;
            else if (this.code != null) this.description = this.code;
        }
        return this;
    }

    public enum RuleType {
        product, order
    }

    public enum ValueType {
        percentage, fixed_amount
    }

    public enum TargetType {
        shipping_line, line_item
    }
}
