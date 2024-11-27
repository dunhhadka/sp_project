package org.example.order.order.application.model.fulfillmentorder.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderLineItemInput;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Builder
public class FulfillmentOrderMoveRequest {
    @NotNull
    private Long newLocationId;
    @NotEmpty
    private List<FulfillmentOrderLineItemInput> fulfillmentOrderLineItems;
}
