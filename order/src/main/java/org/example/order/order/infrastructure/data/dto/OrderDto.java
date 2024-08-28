package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

@Getter
@Setter
public class OrderDto {
    private int storeId;
    private int id;

    private Order.OrderStatus status;
    private Order.FinancialStatus financialStatus;
    private Order.FulfillmentStatus fulfillmentStatus;
    private Order.ReturnStatus returnStatus;

    private Integer totalWeight;
    private String note;

    private String email;
    private String phone;
    private Integer customerId;
    private boolean buyerAcceptMarketing;

    private Integer number;
    private Integer orderNumber;
    private String name;
    private String token;

    private String source;
    private String sourceName;
    private String cartToken;
    private String checkoutToken;
    private String landingSite;
    private String reference;
    private String sourceIdentifier;
    private String sourceUrl;

    private BigDecimal subtotalPrice;
    private BigDecimal totalLineItemPrice;
    private BigDecimal originalTotalPrice;
    private BigDecimal cartDiscountAmount;
    private BigDecimal totalDiscount;
    private BigDecimal totalShippingPrice;
    private BigDecimal totalTax;
    private BigDecimal currentTotalPrice;
    private BigDecimal currentSubtotalPrice;
    private BigDecimal currentTotalDiscount;
    private BigDecimal currentCartDiscountAmount;
    private BigDecimal currentTotalTax;
    private BigDecimal totalOutstanding;
    private BigDecimal unpaidAmount;
    private BigDecimal totalRefund;
    private BigDecimal totalReceived;
    private BigDecimal netPayment;

    private Currency currency;

    private String gateWay;
    private String processingGateWay;

    private boolean taxExempt;
    private boolean taxIncluded;

    private Integer locationId;
    private Instant createdOn;
    private Instant modifiedOn;
    private Instant processOn;
    private Instant closedOn;
    private Instant cancelledOn;

    private Integer version;
}
