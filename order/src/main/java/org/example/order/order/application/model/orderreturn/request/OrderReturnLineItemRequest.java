package org.example.order.order.application.model.orderreturn.request;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.orderreturn.model.OrderReturnLineItem;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class OrderReturnLineItemRequest {
    private @NotNull Integer fulfillmentId;
    private @NotNull Integer fulfillmentLineItemId;
    private @Min(1) int quantity;
    private @Size(max = 255) String returnReasonNote;

    private @NotNull OrderReturnLineItem.OrderReturnReason returnReason;
}
