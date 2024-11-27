package org.example.order.order.application.service.fulfillmentorder;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.model.order.request.AdjustmentRequest;

import java.util.List;

@Getter
@Setter
public class InventoryRequest {
    private List<AdjustmentRequest> adjustments;
}
