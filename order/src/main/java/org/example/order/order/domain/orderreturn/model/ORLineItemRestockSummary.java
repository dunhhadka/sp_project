package org.example.order.order.domain.orderreturn.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "or_line_item_restock_summaries")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ORLineItemRestockSummary {

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "lineItemId", referencedColumnName = "id")
    private OrderReturnLineItem aggRoot;

    @Id
    private int id;

    @Min(1)
    private int storeId;

    @NotNull
    @Positive
    private BigDecimal quantity;
    private Integer locationId;
    private boolean restocked;
}
