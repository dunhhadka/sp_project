package org.example.order.order.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.draftorder.request.DraftOrderCreateRequest;
import org.example.order.order.application.model.draftorder.response.DraftOrderResponse;
import org.example.order.order.application.service.draftorder.DraftOrderWriteService;
import org.example.order.order.infrastructure.configuration.bind.StoreId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/draft_orders")
public class DraftOrderController {

    private final DraftOrderWriteService draftOrderWriteService;


    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public DraftOrderResponse draftOrderCreate(@RequestBody @Valid DraftOrderCreateRequest request, @StoreId Integer storeId) {
        var draftOrderId = draftOrderWriteService.createDraftOrder(storeId, request);
        return null;
    }
//
//    @PostMapping("/calculate")
//    @ResponseStatus(value = HttpStatus.OK)
//    public DraftOrderResponse calculateDraftOrder(@RequestBody @Valid DraftOrderCreateRequest request, @StoreId Integer storeId) {
//        return draftOrderWriteService.calculateDraftOrder(storeId, request);
//    }
}
