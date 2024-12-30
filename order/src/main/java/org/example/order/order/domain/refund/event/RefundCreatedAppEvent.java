package org.example.order.order.domain.refund.event;

import lombok.Getter;
import lombok.ToString;
import org.example.order.order.application.model.order.request.TransactionCreateRequest;
import org.example.order.order.domain.order.model.Order;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Getter
@ToString
public class RefundCreatedAppEvent {
    private final int storeId;
    private final long orderId;

    // FIXME: used for create event
    private final Order order;

    private final long refundId;
    private final Long returnId;
    private final Long userId;
    private final @NotEmpty List<RestockLineItem> restockLineItems;

    private final List<TransactionCreateRequest> transactions;

    private final String reason;

    private final Map<String, String> metaData;

    public RefundCreatedAppEvent(
            Order order, long refundId, Long userId,
            Long returnId, String reason,
            List<RestockLineItem> restockLineItems,
            List<TransactionCreateRequest> transactions,
            Map<String, String> metaData
    ) {
        this.storeId = order.getId().getStoreId();
        this.orderId = order.getId().getId();
        this.order = order;
        this.reason = reason == null ? "refund" : reason;
        this.refundId = refundId;
        this.returnId = returnId;
        this.userId = userId;
        this.restockLineItems = restockLineItems;
        this.transactions = transactions;
        this.metaData = metaData;
    }

    public record RestockLineItem(
            int locationId,
            int lineItemId,
            int quantity,
            boolean isRemoval,
            boolean isRestock
    ) {
    }
}
