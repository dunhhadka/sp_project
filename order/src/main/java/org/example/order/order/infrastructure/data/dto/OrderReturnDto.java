package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.orderreturn.model.OrderReturn.OrderReturnRefundStatus;
import org.example.order.order.domain.orderreturn.model.OrderReturn.OrderReturnRestockStatus;
import org.example.order.order.domain.orderreturn.model.OrderReturn.OrderReturnStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class OrderReturnDto {
    private long id;
    private long storeId;
    private long orderId;
    private long number;
    private Long customerId;
    private Long userId;
    private Long locationId;
    private String name;
    private String note;
    private OrderReturnStatus status;
    private OrderReturnRefundStatus refundStatus;
    private OrderReturnRestockStatus restockStatus;

    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal totalPrice;
    private boolean cancelable;

    private Instant cancelledOn;
    private Instant createdOn;
    private Instant modifiedOn;
    private Instant processedOn;
}
