package org.example.order.order.domain.orderreturn.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.ddd.AggregateRoot;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Getter
@Entity
@Table(name = "order_returns")
@NoArgsConstructor
public class OrderReturn extends AggregateRoot<OrderReturn> {

    @EmbeddedId
    @JsonUnwrapped
    @AttributeOverride(name = "storeId", column = @Column(name = "storeId"))
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    private OrderReturnId id;

    @Min(1)
    private int orderId;

    @Min(1)
    private Integer customerId;

    @Min(1)
    private Integer userId;

    @Min(1)
    private Integer locationId;

    @NotBlank
    @Size(max = 50)
    private String name;

    @Min(1)
    private int number;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private OrderReturnStatus status;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private OrderReturnRefundStatus refundStatus;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private OrderReturnRestockStatus restockStatus;

    @NotNull
    @Min(0)
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @NotNull
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @NotNull
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private boolean cancelable;

    private Instant cancelledOn;

    @Size(max = 500)
    private String note;

    @NotEmpty
    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @OrderBy("id desc")
    @Fetch(FetchMode.SUBSELECT)
    private Set<OrderReturnLineItem> lineItems;

    public enum OrderReturnStatus {
        open, closed, canceled
    }

    public enum OrderReturnRefundStatus {
        unrefund, partial, refunded
    }

    public enum OrderReturnRestockStatus {
        unrestock, partial, restock
    }
}
