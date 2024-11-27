package org.example.order.order.application.model.orderreturn.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class OrderReturnRequest {
    private @Min(1) int orderId;

    private String name;

    private int locationId;

    @NotEmpty
    private List<@Valid OrderReturnLineItemRequest> lineItems;

    private String note;

    private Instant processedOn;

    private Boolean notifyCustomer;
}
