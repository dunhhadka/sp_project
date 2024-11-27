package org.example.order.order.domain.orderreturn.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "order_return_line_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderReturnLineItem {

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderReturnId", referencedColumnName = "id")
    private OrderReturn aggRoot;

    @Id
    private int id;

    @Min(1)
    private int orderId;

    @Min(1)
    private int fulfillmentId;

    @Min(1)
    private int fulfillmentLineItemId;

    @Min(1)
    private int lineItemId;

    @Min(1)
    private int combinationId;

    @NotNull
    private BigDecimal price = BigDecimal.ZERO;
    @NotNull
    private BigDecimal discountedPrice = BigDecimal.ZERO;
    @NotNull
    private BigDecimal discountedSubtotal = BigDecimal.ZERO;
    @NotNull
    private BigDecimal totalCartDiscountAmount = BigDecimal.ZERO;
    @NotNull
    private BigDecimal totalTax = BigDecimal.ZERO;
    @NotNull
    private BigDecimal subtotal = BigDecimal.ZERO;

    @NotNull
    @Min(0)
    private BigDecimal quantity = BigDecimal.ZERO;
    @NotNull
    @Min(0)
    private BigDecimal refundableQuantity = BigDecimal.ZERO;
    @NotNull
    @Min(0)
    private BigDecimal refundedQuantity = BigDecimal.ZERO;
    @NotNull
    @Min(0)
    private BigDecimal restockableQuantity = BigDecimal.ZERO;
    @NotNull
    @Min(0)
    private BigDecimal restockedQuantity = BigDecimal.ZERO;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private OrderReturnReason returnReason;

    @Size(max = 500)
    private String returnReasonNote;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id desc")
    private List<ORLineItemRestockSummary> restockSummaries = new ArrayList<>();

    @NotNull
    private Instant createdOn;

    private Instant modifiedOn;

    @Version
    private Integer version;

    public enum OrderReturnReason {
        unknown,
        unwanted,
        not_as_described,
        wrong_item,
        defective,
        faulty,
        size,
        color,
        style,
        other
    }
}
