package org.example.order.order.domain.orderedit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "order_edit_discount_allocations")
public class AddedDiscountAllocation {
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "editing_id", referencedColumnName = "id")
    @JoinColumn(name = "store_id", referencedColumnName = "store_id")
    private OrderEdit aggRoot;

    @Id
    private UUID id;

    @NotNull
    private String applicationId;

    @NotNull
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    private UUID lineItemId;

    @NotNull
    private Instant updatedAt;
}
