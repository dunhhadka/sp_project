package org.example.order.order.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.OrderEditResponse;
import org.example.order.order.application.service.orderedit.OrderEditReadService;
import org.example.order.order.application.service.orderedit.OrderEditWriterService;
import org.example.order.order.infrastructure.configuration.bind.StoreId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order-edits")
public class OrderEditController {
    private final OrderEditWriterService orderEditWriterService;
    private final OrderEditReadService orderEditReadService;

    @PostMapping("/begin-edit/{orderId}")
    public OrderEditResponse beginEdit(@StoreId Integer storeId, @PathVariable int orderId) {
        var editId = orderEditWriterService.beginEdit(storeId, orderId);
        return orderEditReadService.getBeginEdit(editId);
    }
}