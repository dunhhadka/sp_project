package org.example.order.order.application.model.order.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.request.TransactionCreateRequest;
import org.example.order.order.application.model.order.response.OrderRoutingResponse;
import org.example.order.order.application.service.order.OrderPaymentResult;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.infrastructure.data.dto.StoreDto;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderCreatedEvent {
    private StoreDto store;
    private OrderId orderId;
    private OrderRoutingResponse orderRoutingResponse;
    private List<OrderCreateRequest.FulfillmentRequest> fulfillmentRequests;
    private OrderPaymentResult orderPaymentResult;
    private List<TransactionCreateRequest> transactionRequests;
}
