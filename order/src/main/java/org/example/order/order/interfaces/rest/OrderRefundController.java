package org.example.order.order.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.order.request.RefundRequest;
import org.example.order.order.application.service.order.OrderWriteService;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.infrastructure.configuration.bind.StoreId;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/amin/orders/{order_id}/refunds")
public class OrderRefundController {

    private final OrderWriteService orderWriteService;

    @PostMapping
    public void createRefund(
            @StoreId Integer storeId,
            @PathVariable("order_id") int orderId,
            @Valid @RequestBody RefundRequest request
    ) {
        var refundId = orderWriteService.createRefund(new OrderId(storeId, orderId), request);
    }
}
