package org.example.order.order.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderreturn.request.OrderReturnRequest;
import org.example.order.order.application.model.orderreturn.response.OrderReturnResponse;
import org.example.order.order.application.service.orderreturn.OrderReturnWriteService;
import org.example.order.order.infrastructure.configuration.bind.StoreId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/order-returns")
public class OrderReturnController {

    private final OrderReturnWriteService orderReturnWriteService;

    @PostMapping
    public OrderReturnResponse create(
            @StoreId int storeId,
            @RequestBody @Valid OrderReturnRequest request
    ) {
        var orderReturnId = orderReturnWriteService.create(storeId, request);
        return null;
    }
}
