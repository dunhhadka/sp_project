package org.example.order.order.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.example.order.order.application.model.orderedit.OrderEditResponse;
import org.example.order.order.application.service.orderedit.OrderEditReadService;
import org.example.order.order.application.service.orderedit.OrderEditRequest;
import org.example.order.order.application.service.orderedit.OrderEditWriteService;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.infrastructure.configuration.bind.StoreId;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/order_edit")
public class OrderEditController {

    private final OrderEditWriteService orderEditWriteService;
    private final OrderEditReadService orderEditReadService;

    @PostMapping("/{id}")
    public OrderEditResponse beginEdit(@PathVariable int id, @StoreId Integer storeId) {
        var orderId = new OrderId(storeId, id);
        var orderEditId = orderEditWriteService.beginEdit(orderId);
        return orderEditReadService.getBeginEdit(orderEditId);
    }

    @PostMapping("/add-variants/{id}")
    public OrderEditResponse addVariants(@PathVariable int id,
                                         @StoreId Integer storeId,
                                         @RequestBody @Valid OrderEditRequest.AddVariants addVariants) {
        var orderEditId = new OrderEditId(storeId, id);
        List<UUID> lineItemIds = orderEditWriteService.addVariants(orderEditId, addVariants);
        return orderEditReadService.getAddVariants(orderEditId, lineItemIds);
    }

    @PostMapping("/increment/{editId}")
    public OrderEditResponse increaseLineItemQuantity(@PathVariable int editId,
                                                      @StoreId Integer storeId,
                                                      @RequestBody @Valid OrderEditRequest.Increment increment
    ) {
        var orderEditId = new OrderEditId(storeId, editId);
        var lineItemId = orderEditWriteService.increaseLineItem(orderEditId, increment);
        return orderEditReadService.getBeginEdit(orderEditId);
    }

    @PostMapping("/{id}/set_item_quantity")
    public OrderEditResponse setItemQuantity(
            @StoreId Integer storeId,
            @PathVariable int id,
            @RequestBody @Valid OrderEditRequest.SetItemQuantity request
    ) {
        var orderEditId = new OrderEditId(storeId, id);
        var lineItemId = orderEditWriteService.setItemQuantity(orderEditId, request);
        return orderEditReadService.getBeginEdit(orderEditId);
    }

    @PostMapping("/{id}/commit")
    public OrderEditResponse commit(@StoreId Integer storeId,
                                    @PathVariable int id
    ) {
        var orderEditId = new OrderEditId(storeId, id);
        orderEditWriteService.commit(orderEditId);
        return orderEditReadService.getBeginEdit(orderEditId);
    }
}
