package org.example.order.order.application.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.model.order.request.OrderFilterRequest;
import org.example.order.order.application.model.order.response.CombinationLineComponentResponse;
import org.example.order.order.application.model.order.response.CombinationLineResponse;
import org.example.order.order.application.model.order.response.LineItemResponse;
import org.example.order.order.application.model.order.response.OrderResponse;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.DiscountAllocation;
import org.example.order.order.domain.order.model.TaxLine;
import org.example.order.order.infrastructure.data.dao.*;
import org.example.order.order.infrastructure.data.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReadService {

    private final OrderDao orderDao;
    private final CombinationDao combinationDao;
    private final LineItemDao lineItemDao;
    private final ShippingLineDao shippingLineDao;
    private final OrderDiscountDao orderDiscountDao;
    private final DiscountAllocationDao discountAllocationDao;
    private final DiscountApplicationDao discountApplicationDao;
    private final TaxLineDao taxLineDao;
    private final ShippingAddressDao shippingAddressDao;
    private final BillingAddressDao billingAddressDao;
    private final OrderTagDao orderTagDao;

    private final OrderMapper orderMapper;

    public List<OrderResponse> filterOrders(Integer storeId, OrderFilterRequest request) {
        log.info("debug");
        Objects.requireNonNull(storeId);
        var orders = orderDao.filter(storeId, request);
        return getOrderResponse(storeId, orders);
    }

    private List<OrderResponse> getOrderResponse(Integer storeId, List<OrderDto> orders) {
        if (CollectionUtils.isEmpty(orders)) return List.of();

        var orderIds = orders.stream().map(OrderDto::getId).toList();

        var combinationLines = combinationDao.getByOrderIds(storeId, orderIds);
        var lineItems = lineItemDao.getByOrderIds(storeId, orderIds);
        var shippingLines = shippingLineDao.getByOrderIds(storeId, orderIds);
        var discountCodes = orderDiscountDao.getByOrderIds(storeId, orderIds);
        var discountAllocations = discountAllocationDao.getByOrderIds(storeId, orderIds);
        var discountApplications = discountApplicationDao.getByOrderIds(storeId, orderIds);
        var taxLines = taxLineDao.getByOrderIds(storeId, orderIds);
        var shippingAddresses = shippingAddressDao.getByOrderIds(storeId, orderIds);
        var billingAddresses = billingAddressDao.getByOrderIds(storeId, orderIds);
        var orderTags = orderTagDao.getByOrderIds(storeId, orderIds);

        var taxLine2DMap = taxLines.stream()
                .collect(Collectors.groupingBy(TaxLineDto::getTargetType,
                        Collectors.groupingBy(TaxLineDto::getTargetId)));
        var discountApplication2DMap = discountAllocations.stream()
                .collect(Collectors.groupingBy(DiscountAllocationDto::getTargetType,
                        Collectors.groupingBy(DiscountAllocationDto::getTargetId)));

        var orderListResponse = new ArrayList<OrderResponse>(orders.size());
        for (var order : orders) {
            var orderResponse = orderMapper.fromDtoToResponse(order);
            orderListResponse.add(orderResponse);

            //lineItems
            var lineItemOfOrder = lineItems.stream()
                    .filter(lineItem -> lineItem.getOrderId() == order.getId())
                    .toList();
            var lineItemResponses = lineItemOfOrder.stream()
                    .map(lineItem -> {
                        LineItemResponse lineItemResponse = orderMapper.fromDtoToResponse(lineItem);

                        var taxLineResponses = getValueByKeys(taxLine2DMap, TaxLine.TargetType.line_item, lineItem.getId(), List.of()).stream()
                                .map(orderMapper::fromDtoToResponse)
                                .toList();
                        lineItemResponse.setTaxLines(taxLineResponses);

                        var discountAllocationResponses = getValueByKeys(discountApplication2DMap, DiscountAllocation.TargetType.line_item, lineItem.getId(), List.of()).stream()
                                .map(orderMapper::fromDtoToResponse)
                                .toList();
                        lineItemResponse.setDiscountAllocations(discountAllocationResponses);

                        return lineItemResponse;
                    }).toList();
            orderResponse.setLineItems(lineItemResponses);

            // shippingLines
            var shippingLineResponses = shippingLines.stream()
                    .filter(shippingLine -> shippingLine.getOrderId() == order.getId())
                    .sorted(Comparator.comparingInt(ShippingLineDto::getId))
                    .map(shippingLine -> {
                        var shippingLineResponse = orderMapper.fromDtoToResponse(shippingLine);

                        var taxLineResponses = getValueByKeys(taxLine2DMap, TaxLine.TargetType.shipping_line, shippingLine.getId(), List.of()).stream()
                                .map(orderMapper::fromDtoToResponse)
                                .toList();
                        shippingLineResponse.setTaxLines(taxLineResponses);

                        var discountAllocationResponses = getValueByKeys(discountApplication2DMap, DiscountAllocation.TargetType.shipping_line, shippingLine.getId(), List.of()).stream()
                                .map(orderMapper::fromDtoToResponse)
                                .toList();
                        shippingLineResponse.setDiscountAllocations(discountAllocationResponses);
                        return shippingLineResponse;
                    })
                    .toList();
            orderResponse.setShippingLines(shippingLineResponses);

            // orderDiscounts
            var discountCodeResponses = discountCodes.stream()
                    .filter(d -> d.getOrderId() == order.getId())
                    .map(orderMapper::fromDtoToResponse)
                    .toList();
            orderResponse.setOrderDiscountResponses(discountCodeResponses);

            var discountApplicationResponses = discountApplications.stream()
                    .filter(d -> d.getOrderId() == order.getId())
                    .sorted(Comparator.comparingInt(DiscountApplicationDto::getApplyIndex))
                    .map(orderMapper::fromDtoToResponse)
                    .toList();
            orderResponse.setDiscountApplications(discountApplicationResponses);

            shippingAddresses.stream()
                    .filter(s -> s.getOrderId() == order.getId())
                    .map(orderMapper::fromDtoToResponse)
                    .findFirst()
                    .ifPresent(orderResponse::setShippingAddress);

            billingAddresses.stream()
                    .filter(s -> s.getOrderId() == order.getId())
                    .map(orderMapper::fromDtoToResponse)
                    .findFirst()
                    .ifPresent(orderResponse::setBillingAddress);

            var tags = orderTags.stream()
                    .filter(t -> t.getOrderId() == order.getId())
                    .map(OrderTagDto::getValue)
                    .collect(Collectors.joining(", "));
            orderResponse.setTags(tags);

            var combinationLineResponses = combinationLines.stream()
                    .filter(c -> c.getOrderId() == order.getId())
                    .map(orderMapper::fromDtoToResponse)
                    .toList();
            mapComponentForCombinationLines(combinationLineResponses, lineItemResponses);
            orderResponse.setCombinationLines(combinationLineResponses);
        }

        return orderListResponse;
    }

    private void mapComponentForCombinationLines(
            List<CombinationLineResponse> combinationLineResponses,
            List<LineItemResponse> lineItemResponses
    ) {
        if (CollectionUtils.isEmpty(combinationLineResponses)) return;

        for (var item : combinationLineResponses) {
            var lineItemOfCombinations = lineItemResponses.stream()
                    .filter(line -> line.getCombinationLineId() != null && line.getCombinationLineId() == item.getId())
                    .toList();
            Map<String, CombinationLineComponentResponse> components = new HashMap<>();
            for (var lineItem : lineItemOfCombinations) {
                var existedComponent = components.get(lineItem.getCombinationLineKey());
                if (existedComponent != null) {
                    continue;
                }
                var quantity = lineItemOfCombinations.stream()
                        .filter(line -> line.getCombinationLineKey().equals(lineItem.getCombinationLineKey()))
                        .mapToLong(LineItemResponse::getQuantity)
                        .sum();
                var component = orderMapper.toCombinationLineComponentResponse(lineItem);
                var combineQuantity = NumberUtils.isPositive(item.getQuantity()) ? item.getQuantity() : BigDecimal.ZERO;
                component.setQuantity(BigDecimal.valueOf(quantity).divide(combineQuantity, 3, RoundingMode.FLOOR));
                components.put(lineItem.getCombinationLineKey(), component);
            }
            item.setComponents(components.values().stream().toList());
        }
    }

    private <R, C, V> V getValueByKeys(Map<R, Map<C, V>> map, R rowKey, C columnKey, V defaultValue) {
        var columnValues = map.get(rowKey);
        if (columnValues == null) return defaultValue;
        var values = columnValues.get(columnKey);
        return values == null ? defaultValue : values;
    }
}
