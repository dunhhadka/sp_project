package org.example.order.order.application.model.order.response;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.Order.FinancialStatus;
import org.example.order.order.domain.order.model.Order.FulfillmentStatus;
import org.example.order.order.domain.order.model.Order.OrderStatus;
import org.example.order.order.domain.order.model.Order.ReturnStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

@Getter
@Setter
public class OrderResponse {
    private int id;
    private int storeId;
    private boolean buyerAcceptMarketing;
    private String cancelReason;
    private Instant cancelledOn;
    private String cartToken;
    private String checkoutToken;
    private Instant closedOn;
    private Instant createdOn;
    private Currency currency;
    private String email;
    private String phone;
    private FulfillmentStatus fulfillmentStatus;
    private FinancialStatus financialStatus;
    private OrderStatus status;
    private ReturnStatus returnStatus;
    private String name;
    private String note;
    private Integer number;
    private Integer orderNumber;
    private Instant processedOn;
    private String processingMethod;
    private String sourceUrl;
    private String sourceName;
    private String source;
    private String landingSite;
    private String landingSiteRef;
    private String referringSite;
    private String reference;
    private String sourceIdentifier;
    private String gateway;
    private String token;
    private BigDecimal subtotalPrice;
    private BigDecimal totalDiscounts;
    private BigDecimal totalLineItemsPrice;
    private BigDecimal totalPrice;
    private Integer totalWeight;
    private Instant modifiedOn;
    private String tags;

    private OrderAddressResponse billingAddress;
    private OrderAddressResponse shippingAddress;
    private List<LineItemResponse> lineItems;
    private List<ShippingLineResponse> shippingLines;
    private List<DiscountApplicationResponse> discountApplications;
    private List<CombinationLineResponse> combinationLines;
    private List<OrderDiscountResponse> orderDiscountResponses;

    private Integer locationId;

    private List<String> paymentGatewayNames;
}
