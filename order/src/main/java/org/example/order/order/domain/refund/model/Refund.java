package org.example.order.order.domain.refund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.order.domain.order.model.Order;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.function.Function;

@Getter
@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    @JsonIgnore
    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "storeId", referencedColumnName = "storeId"),
            @JoinColumn(name = "orderId", referencedColumnName = "id")
    })
    private Order aggRoot;

    @Id
    private int id;

    @NotNull
    @Column(columnDefinition = "DATETIME2")
    private Instant createdOn;

    @Column(columnDefinition = "DATETIME2")
    private Instant processedAt;

    private Integer userId;

    private Long returnId;

    @Size(max = 1000)
    private String note;

    @Setter
    private boolean restock;

    private BigDecimal totalRefund;

    private BigDecimal totalRefunded;

    @NotNull
    private BigDecimal outstandingAdjustmentAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private Set<RefundLineItem> refundLineItems;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private Set<OrderAdjustment> orderAdjustments;

    @Version
    private Integer version;

    public Refund(
            int id,
            Set<RefundLineItem> refundLineItems,
            Set<OrderAdjustment> orderAdjustments,
            String note,
            Instant processedAt
    ) {
        this.id = id;
        this.refundLineItems = refundLineItems;
        this.orderAdjustments = orderAdjustments;
        this.note = note;
        this.processedAt = processedAt;
    }

    @JsonIgnore
    public BigDecimal getTotalCartDiscountRefunded() {
        if (!CollectionUtils.isEmpty(this.refundLineItems)) {
            return this.refundLineItems.stream()
                    .map(RefundLineItem::getTotalCartDiscount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    public void setReturnId(long orderReturnId) {
        this.returnId = orderReturnId;
    }

    public Refund setTotalRefunded(BigDecimal totalRefunded) {
        this.totalRefunded = totalRefunded;
        return this;
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(this.refundLineItems)
                && CollectionUtils.isEmpty(this.orderAdjustments);
    }

    @JsonIgnore
    public BigDecimal getLineItemSubtotalRefunded() {
        if (!CollectionUtils.isEmpty(this.refundLineItems)) {
            return this.refundLineItems.stream()
                    .map(RefundLineItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    @JsonIgnore
    public BigDecimal getTotalLineItemTaxRefunded() {
        if (!CollectionUtils.isEmpty(this.refundLineItems)) {
            return this.refundLineItems.stream()
                    .map(RefundLineItem::getTotalTax)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    @JsonIgnore
    public BigDecimal getRefundedOriginalPrice() {
        if (CollectionUtils.isEmpty(this.refundLineItems)) return BigDecimal.ZERO;
        return this.refundLineItems.stream()
                .map(RefundLineItem::getTotalOriginalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @JsonIgnore
    public BigDecimal getTotalShippingRefunded() {
        return getTotalAdjustmentFilter(oa -> {
            if (!this.aggRoot.isTaxIncluded()) return oa.getAmount();
            return oa.getAmount().add(oa.getTaxAmount());
        });
    }

    @JsonIgnore
    public BigDecimal getTotalShippingTaxRefunded() {
        return getTotalAdjustmentFilter(OrderAdjustment::getTaxAmount);
    }

    @JsonIgnore
    public BigDecimal getTotalAdjustmentFilter(Function<OrderAdjustment, BigDecimal> amountType) {
        if (CollectionUtils.isEmpty(this.orderAdjustments)) return BigDecimal.ZERO;
        return this.orderAdjustments.stream()
                .filter(oa -> oa.getRefundKind() == OrderAdjustment.RefundKind.shipping_refund)
                .map(amountType)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @JsonIgnore
    public BigDecimal getDiscrepancyAmount() {
        if (CollectionUtils.isEmpty(this.orderAdjustments)) return BigDecimal.ZERO;
        // NOTE: kind = refund_discrepancy => tax_amount = 0 (always)
        return this.orderAdjustments.stream()
                .filter(r -> OrderAdjustment.RefundKind.refund_discrepancy == r.getRefundKind())
                .map(OrderAdjustment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
