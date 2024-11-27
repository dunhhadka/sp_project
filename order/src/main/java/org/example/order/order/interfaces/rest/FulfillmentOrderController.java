package org.example.order.order.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.fulfillmentorder.request.FulfillmentOrderMoveRequest;
import org.example.order.order.application.service.fulfillment.FulfillmentOrderWriteService;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.infrastructure.configuration.bind.StoreId;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/fulfillment_orders")
public class FulfillmentOrderController {

    private final FulfillmentOrderWriteService fulfillmentOrderWriteService;

    @PostMapping("/{id}/move")
    public void move(
            @PathVariable("id") int id,
            @RequestBody FulfillmentOrderMoveRequest request,
            @StoreId Integer storeId
    ) {
        var fulfillmentOrderId = new FulfillmentOrderId(storeId, id);
        fulfillmentOrderWriteService.move(fulfillmentOrderId, request);
    }
}
