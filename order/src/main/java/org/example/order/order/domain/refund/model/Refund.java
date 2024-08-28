package org.example.order.order.domain.refund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.order.domain.order.model.Order;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Getter
@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
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

    private boolean restock;

    private BigDecimal totalRefund;

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

    public BigDecimal getTotalCartDiscountRefunded() {
        if (!CollectionUtils.isEmpty(this.refundLineItems)) {
            return this.refundLineItems.stream()
                    .map(RefundLineItem::getTotalCartDiscount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }
}
