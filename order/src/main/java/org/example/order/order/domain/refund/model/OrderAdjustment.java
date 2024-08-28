package org.example.order.order.domain.refund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "order_adjustments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAdjustment {

    @JsonIgnore
    @Setter(AccessLevel.PACKAGE)
    @ManyToOne
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "refundId", referencedColumnName = "id")
    private Refund refund;

    @Id
    private int id;

    private @NotNull BigDecimal amount;

    private @NotNull BigDecimal taxAmount = BigDecimal.ZERO;

    @Enumerated(value = EnumType.STRING)
    private RefundKind refundKind;

    @Size(max = 1000)
    private String reason;

    @Version
    private Integer version;

    public enum RefundKind {
        shipping_refund,
        refund_discrepancy
    }
}
