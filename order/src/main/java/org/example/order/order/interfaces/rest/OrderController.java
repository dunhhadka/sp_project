package org.example.order.order.interfaces.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.request.OrderFilterRequest;
import org.example.order.order.application.model.order.response.OrderResponse;
import org.example.order.order.application.service.order.OrderCacheService;
import org.example.order.order.application.service.order.OrderReadService;
import org.example.order.order.application.service.order.OrderWriteService;
import org.example.order.order.infrastructure.configuration.bind.StoreId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderWriteService orderWriteService;
    private final OrderReadService orderReadService;

    private final OrderCacheService orderCacheService;

    @GetMapping
    public List<OrderResponse> filter(@StoreId Integer storeId, OrderFilterRequest request) {
        return orderReadService.filterOrders(storeId, request);
    }

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public OrderResponse create(@StoreId Integer storeId, @RequestBody OrderCreateRequest request) {
        var orderId = orderWriteService.create(storeId, request);
        return null;
    }

    @PostMapping("/cache")
    public void insertToCache() {
        var list = List.of(1, 2, 3, 4);
    }
}
