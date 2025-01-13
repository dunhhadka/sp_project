package org.example.order.order.domain.orderedit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.springframework.data.relational.core.sql.In;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "order_edit_discount_applications")
public class AddedDiscountApplication {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "editing_id", referencedColumnName = "id")
    @JoinColumn(name = "store_id", referencedColumnName = "store_id")
    private OrderEdit aggRoot;

    @Id
    private UUID id;

    @Size(max = 255)
    private String description;

    @NotNull
    @Min(0)
    @Column(name = "[value]")
    private BigDecimal value;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private DiscountApplication.ValueType valueType;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private DiscountApplication.TargetType targetType = DiscountApplication.TargetType.line_item;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private DiscountApplication.RuleType ruleType;

    @NotNull
    private Instant updatedAt;

    public void update(String description, BigDecimal value, DiscountApplication.ValueType type) {
        this.description = description;
        this.value = value;
        this.valueType = type;

        this.updatedAt = Instant.now();
    }
}
