package org.example.order.order.application.service.orderreturn;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.model.order.request.RefundRequest;
import org.example.order.order.application.model.orderreturn.request.OrderReturnLineItemRequest;
import org.example.order.order.application.model.orderreturn.request.OrderReturnRequest;
import org.example.order.order.application.service.order.RefundCalculationService;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.orderreturn.model.OrderReturnId;
import org.example.order.order.domain.orderreturn.model.OrderReturnLineItem;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.infrastructure.data.dao.*;
import org.example.order.order.infrastructure.data.dto.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReturnWriteService {

    private final static String ORDER_RETURN_NAME_FORMAT = "%s-R%s";

    private final OrderDao orderDao;
    private final LineItemDao lineItemDao;
    private final OrderReturnDao orderReturnDao;
    private final FulfillmentDao fulfillmentDao;
    private final FulfillmentLineItemDao fulfillmentLineItemDao;

    private final RefundCalculationService calculationService;

    @Transactional
    public OrderReturnId create(int storeId, OrderReturnRequest request) {
        var orderId = request.getOrderId();
        OrderDto order = null;
        if (orderId > 0) order = orderDao.findById(storeId, orderId);
        if (order == null) throw new NotFoundException("Order not found");

        var fulfillments = fulfillmentDao.getByOrderIds(storeId, List.of(orderId));
        var fulfillmentIds = fulfillments.stream().map(FulfillmentDto::getId).toList();
        var fulfillmentLineItems = fulfillmentLineItemDao.getByFulfillmentIds(storeId, List.of(orderId), fulfillmentIds);
        var orderLineItemIds = fulfillmentLineItems.stream().map(FulfillmentLineItemDto::getLineItemId).filter(NumberUtils::isPositive).distinct().toList();
        var orderLineItems = lineItemDao.getByIds(storeId, orderLineItemIds);
        var orderReturns = orderReturnDao.getByOrderId(storeId, orderId);
        var validRequest = handleOrderReturnLineItemRequest(request.getLineItems(), fulfillments, fulfillmentLineItems);

        var orderReturnName = buildOrderReturnName(request, order, orderReturns);
        var userId = 100;
        var customerId = order.getCustomerId();
        var note = request.getNote();
        var orderReturnLineItems = buildOrderReturnLineItems(storeId, orderId, validRequest, fulfillmentLineItems, orderLineItems);
        return null;
    }

    private Set<OrderReturnLineItem> buildOrderReturnLineItems(
            int storeId,
            int orderId,
            List<OrderReturnLineItemRequest> lineItemRequests,
            List<FulfillmentLineItemDto> fulfillmentLineItems,
            List<LineItemDto> orderLineItems
    ) {
        var fulfillmentLineItemMap = fulfillmentLineItems.stream()
                .collect(Collectors.toMap(FulfillmentLineItemDto::getId, Function.identity()));
        var refundLineItemRequests = lineItemRequests.stream().map(request -> {
            var fulfillmentLineItem = fulfillmentLineItemMap.get(request.getFulfillmentLineItemId());
            // fulfillmentLineItem always not null
            return RefundRequest.LineItem.builder()
                    .lineItemId(fulfillmentLineItem.getLineItemId().intValue())
                    .quantity(request.getQuantity())
                    .restockType(RefundLineItem.RestockType.no_restock)
                    .build();
        }).toList();
        var combinationLineMap = orderLineItems.stream()
                .filter(item -> StringUtils.isNotBlank(item.getCombinationLineKey()))
                .collect(Collectors.toMap(LineItemDto::getId, LineItemDto::getCombinationLineId));
        return null;
    }

    private List<OrderReturnLineItemRequest> handleOrderReturnLineItemRequest(
            List<OrderReturnLineItemRequest> lineItems,
            List<FulfillmentDto> fulfillments,
            List<FulfillmentLineItemDto> fulfillmentLineItems
    ) {
        List<OrderReturnLineItemRequest> validLineRequests = new ArrayList<>();
        for (var requestLineItem : lineItems) {
            var fulfillment = IterableUtils.find(fulfillments,
                    ff -> Objects.equals(ff.getId(), requestLineItem.getFulfillmentId()));
            if (fulfillment == null) {
                throw new ConstrainViolationException("fulfillment_id",
                        "fulfillment_id " + requestLineItem.getFulfillmentId() + " is not exist");
            }
            if (fulfillment.getStatus() != Fulfillment.FulfillStatus.success) {
                throw new ConstrainViolationException("fulfillment_id",
                        "fulfillment_id " + requestLineItem.getFulfillmentId() + " is invalid status");
            }
            var fulfillmentLineItem = IterableUtils.find(fulfillmentLineItems,
                    lineItem -> lineItem.getId().equals(requestLineItem.getFulfillmentLineItemId())
                            && Objects.equals(lineItem.getFulfillmentId(), fulfillment.getId()));
            if (fulfillmentLineItem == null) {
                throw new ConstrainViolationException("fulfillment_line_item_id",
                        "fulfillment_line_item_id " + requestLineItem.getFulfillmentLineItemId() + " is not exist");
            }
            if (requestLineItem.getQuantity() > fulfillmentLineItem.getEffectiveQuantity()) {
                throw new ConstrainViolationException("line_item",
                        "quantity must be less or equal returnable_quantity");
            }
            if (!requestLineItem.getReturnReason().equals(OrderReturnLineItem.OrderReturnReason.other)
                    && StringUtils.isNotBlank(requestLineItem.getReturnReasonNote())) {
                throw new ConstrainViolationException("line_item",
                        "reason note is not allowed with the reason being " + requestLineItem.getReturnReason().name());
            }
            validLineRequests.add(requestLineItem);
        }
        return validLineRequests;
    }

    private String buildOrderReturnName(OrderReturnRequest request, OrderDto order, List<OrderReturnDto> orderReturns) {
        if (StringUtils.isNotBlank(request.getName())) return request.getName();
        var orderName = order.getName();
        var count = CollectionUtils.isNotEmpty(orderReturns) ? orderReturns.size() + 1 : 1;
        return String.format(ORDER_RETURN_NAME_FORMAT, orderName, count);
    }
}
