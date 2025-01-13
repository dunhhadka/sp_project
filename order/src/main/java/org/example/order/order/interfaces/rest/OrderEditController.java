package org.example.order.order.interfaces.rest;

import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.OrderEditResponse;
import org.example.order.order.application.service.orderedit.OrderEditReadService;
import org.example.order.order.application.service.orderedit.OrderEditRequest;
import org.example.order.order.application.service.orderedit.OrderEditWriterService;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.infrastructure.configuration.bind.StoreId;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

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

    @PostMapping("/{id}/add_variants")
    public OrderEditResponse addVariants(@StoreId Integer storeId, @PathVariable int id,
                                         @RequestBody @Valid OrderEditRequest.AddVariants addVariants) {
        var orderEditId = new OrderEditId(storeId, id);
        List<UUID> addedLineItemIds = orderEditWriterService.addVariants(orderEditId, addVariants);
        return orderEditReadService.getEditWithAddedLines(orderEditId, addedLineItemIds);
    }

    @PostMapping("/{id}/add_custom_item")
    public OrderEditResponse addCustomItem(@StoreId Integer storeId, @PathVariable int id,
                                           @RequestBody @Valid OrderEditRequest.AddCustomItem request) {
        var orderEditId = new OrderEditId(storeId, id);
        UUID addedCustomItemId = orderEditWriterService.addCustomItem(orderEditId, request);
        return orderEditReadService.getEditWithAddedLines(orderEditId, List.of(addedCustomItemId));
    }

    @PostMapping("/{id}/set_item_quantity")
    public OrderEditResponse setLineItemQuantity(@StoreId Integer storeId, @PathVariable int id,
                                                 @RequestBody @Valid OrderEditRequest.SetItemQuantity request) {
        var orderEditId = new OrderEditId(storeId, id);
        String lineItemId = orderEditWriterService.updateQuantity(orderEditId, request);
        return orderEditReadService.getCalculatedLineItem(orderEditId, lineItemId);
    }

    @PostMapping("/{id}/set_item_discount")
    public OrderEditResponse setLineItemDiscount(@StoreId Integer storeId, @PathVariable int id,
                                                 @RequestBody @Valid OrderEditRequest.SetItemDiscount request) {
        var orderEditId = new OrderEditId(storeId, id);
        orderEditWriterService.applyDiscount(orderEditId, request);
        return null;
    }
}