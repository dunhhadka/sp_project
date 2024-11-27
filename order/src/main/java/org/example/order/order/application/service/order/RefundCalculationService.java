package org.example.order.order.application.service.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.model.order.request.RefundRequest;
import org.example.order.order.application.model.order.response.RefundCalculateResponse;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.infrastructure.data.dao.FulfillmentOrderDao;
import org.example.order.order.infrastructure.data.dao.FulfillmentOrderLineItemDao;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dto.FulfillmentOrderDto;
import org.example.order.order.infrastructure.data.dto.FulfillmentOrderLineItemDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundCalculationService {

    private final ProductDao productDao;
    private final FulfillmentOrderDao fulfillmentOrderDao;
    private final FulfillmentOrderLineItemDao fulfillmentOrderLineItemDao;

    public RefundCalculateResponse calculateRefund(Order order, RefundRequest refundRequest) {
        var shipping = suggestRefundShipping(order, refundRequest.getShipping());
        var refundItemResult = suggestRefundLineItems(order, refundRequest);

        return null;
    }

    private RefundItemResult suggestRefundLineItems(Order order, RefundRequest refundRequest) {
        var refundItemRequests = safeSelect(refundRequest.getRefundLineItems(), (line) -> NumberUtils.isPositive(line.getLineItemId()));
        if (refundItemRequests.isEmpty()) {
            // tại sao lại thêm option create => tạo refund tất cả line_items ?
            if (refundRequest.getOption().isCreate()) {
                var refundableLineItems = getRefundableLineItems(order);
                return new RefundItemResult(refundableLineItems, List.of());
            }
            return RefundItemResult.EMPTY();
        }

        return suggestRefundLineItems(order, refundItemRequests, refundRequest.getOption());
    }

    private RefundItemResult suggestRefundLineItems(Order order, List<RefundRequest.LineItem> refundItemRequests, RefundRequest.Option option) {
        var refundableLineItems = getRefundableLineItems(order);
        // bắt bược restock_type
        forceRestockType(order, refundableLineItems, refundItemRequests);

        validateRefundItem(order.getLineItems(), refundableLineItems, refundItemRequests);

        validateLocation(order.getId(), refundItemRequests);

        refundItemRequests = chooseRefundTypeForLineItem(order, refundableLineItems, refundItemRequests);

        var refundLineItems = calculateRefundLineItemInfo(order, refundableLineItems, refundItemRequests);

        return new RefundItemResult(refundableLineItems, refundLineItems);
    }

    private List<RefundCalculateResponse.LineItem> calculateRefundLineItemInfo(
            Order order,
            List<RefundCalculateResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> refundItemRequests
    ) {
        var calculationLineItems = new ArrayList<RefundCalculateResponse.LineItem>();
        Map<Integer, Integer> refundedQuantityCache = new HashMap<>();
        for (var requestLine : refundItemRequests) {
            var refundableLine = refundableLineItems.stream()
                    .filter(line -> line.getLineItemId() == requestLine.getLineItemId())
                    .findFirst().orElseThrow(() -> new ConstrainViolationException("", ""));

            var lineItem = refundableLine.getLineItem();
            var suggestQuantity = Math.min(refundableLine.getMaximumRefundableQuantity(), requestLine.getQuantity());
            var refundedQuantity = lineItem.getQuantity() - refundableLine.getMaximumRefundableQuantity();

            int localRefundedQuantity = refundedQuantityCache.getOrDefault(lineItem.getId(), 0);
            refundedQuantity += localRefundedQuantity;

            var lineItemDiscountDetails = categorizeLineItemDiscount(lineItem, order);
            var totalCartDiscount = lineItemDiscountDetails.getLeft();
            var totalProductDiscount = lineItemDiscountDetails.getRight();

            var totalTax = lineItem.getTaxLines().stream()
                    .map(TaxLine::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var calculateRefundLine = refundableLine.copy();
            calculationLineItems.add(calculateRefundLine);

            calculateRefundLine.setQuantity(suggestQuantity);

            calculateRefundLine
                    .setLocationId(requestLine.getLocationId())
                    .setRestockType(requestLine.getRestockType())
                    .setRemoval(requestLine.isRemoval());

            var isRefundAllRemaining = suggestQuantity == refundableLine.getMaximumRefundableQuantity();
            if (isRefundAllRemaining && refundedQuantity == 0) {
                this.suggestRefundPrice(
                        calculateRefundLine,
                        lineItem.getPrice(),
                        totalProductDiscount,
                        totalCartDiscount,
                        totalTax,
                        lineItem.getQuantity()
                );
            } else {

            }
        }
        return calculationLineItems;
    }

    private void suggestRefundPrice(
            RefundCalculateResponse.LineItem calculateRefundLine,
            BigDecimal lineItemPrice,
            BigDecimal totalProductDiscount,
            BigDecimal totalCartDiscount,
            BigDecimal totalTax,
            int lineQuantity
    ) {
        var quantity = BigDecimal.valueOf(lineQuantity);
        var discountSubtotal = lineItemPrice.multiply(quantity).subtract(totalProductDiscount);
        var discountedUnitPrice = discountSubtotal.divide(quantity, RoundingMode.FLOOR);
        var subtotal = discountSubtotal.subtract(totalCartDiscount);

        calculateRefundLine
                .setSubtotal(subtotal)
                .setTotalTax(totalTax)
                .setTotalCartDiscount(totalCartDiscount)
                .setDiscountedPrice(discountedUnitPrice)
                .setDiscountedSubtotal(discountSubtotal);
    }

    private Pair<BigDecimal, BigDecimal> categorizeLineItemDiscount(LineItem lineItem, Order order) {
        if (CollectionUtils.isNotEmpty(order.getDiscountApplications())) {
            BigDecimal totalCartDiscount = BigDecimal.ZERO;
            BigDecimal totalProductDiscount = BigDecimal.ZERO;
            for (var allocation : lineItem.getDiscountAllocations()) {
                var isProductDiscount = this.filterProductDiscount(allocation, order);
                if (isProductDiscount) totalProductDiscount = totalProductDiscount.add(allocation.getAmount());
                else totalCartDiscount = totalCartDiscount.add(allocation.getAmount());
            }
            return Pair.of(totalCartDiscount, totalProductDiscount);
        }
        return Pair.of(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private boolean filterProductDiscount(DiscountAllocation allocation, Order order) {
        return true;
    }

    private List<RefundRequest.LineItem> chooseRefundTypeForLineItem(
            Order order,
            List<RefundCalculateResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> refundItemRequests
    ) {
        var refundedDetails = getRefundedDetails(order.getRefunds());
        var restockState = refundableLineItems.stream()
                .collect(Collectors.toMap(
                        RefundCalculateResponse.LineItem::getLineItemId,
                        item -> {
                            int removableQuantity = item.getLineItem().getFulfillableQuantity();
                            var refunded = refundedDetails.get(item.getLineItemId());
                            if (refunded != null) {
                                removableQuantity = Math.min(
                                        item.getLineItem().getFulfillableQuantity(),
                                        item.getMaximumRefundableQuantity());
                            }
                            return new RestockItemContext(
                                    item.getMaximumRefundableQuantity(),
                                    removableQuantity,
                                    item.getMaximumRefundableQuantity() - removableQuantity
                            );
                        }
                ));

        var processedRequests = new ArrayList<RefundRequest.LineItem>();
        for (var refundItemRequest : refundItemRequests) {
            var state = restockState.get(refundItemRequest.getLineItemId());
            switch (refundItemRequest.getRestockType()) {
                case no_restock -> {
                    var splitLineItems = splitLineItem(refundItemRequest, state);
                    processedRequests.addAll(splitLineItems);
                }
                case cancel -> {
                    // chỉ cần trừ số lượng trong context
                    refundItemRequest.setRemoval(true);
                    state.reduce(refundItemRequest.getQuantity(), true);
                    processedRequests.add(refundItemRequest);
                }
                case _return -> {
                    refundItemRequest.setRemoval(false);
                    state.reduce(refundItemRequest.getQuantity(), false);
                    processedRequests.add(refundItemRequest);
                }
            }
        }
        return processedRequests;
    }

    private List<RefundRequest.LineItem> splitLineItem(RefundRequest.LineItem original, RestockItemContext state) {
        var refundLineItems = new ArrayList<RefundRequest.LineItem>();
        var executeQuantity = state.reduce(original.getQuantity(), original.isRemoval());
        if (executeQuantity > 0) {
            var returnRequest = original.toBuilder()
                    .removal(original.isRemoval())
                    .quantity(executeQuantity)
                    .build();
            refundLineItems.add(returnRequest);
        }
        if (executeQuantity < original.getQuantity()) {
            var restockType = switch (original.getRestockType()) {
                case no_restock -> RefundLineItem.RestockType.no_restock;
                case _return -> RefundLineItem.RestockType.cancel;
                case cancel -> RefundLineItem.RestockType._return;
                default -> throw new IllegalArgumentException("not supported for restock type");
            };
            var splitQuantity = state.reduce(original.getQuantity() - executeQuantity, !original.isRemoval());
            var cancelRequest = original.toBuilder()
                    .restockType(restockType)
                    .quantity(splitQuantity)
                    .removal(!original.isRemoval())
                    .build();
            refundLineItems.add(cancelRequest);
        }
        return refundLineItems;
    }

    private Map<Integer, RefundItemContext> getRefundedDetails(Set<Refund> refunds) {
        Map<Integer, RefundItemContext> itemContextMap = new HashMap<>();
        for (var refund : refunds) {
            if (CollectionUtils.isEmpty(refund.getRefundLineItems())) {
                continue;
            }
            for (var refundLine : refund.getRefundLineItems()) {
                var itemContext = itemContextMap.get(refundLine.getLineItemId());
                if (itemContext == null) {
                    itemContext = new RefundItemContext(refundLine);
                    itemContextMap.put(refundLine.getLineItemId(), itemContext);
                    continue;
                }
                itemContext.add(refundLine);
            }
        }
        return itemContextMap;
    }

    private void validateLocation(OrderId orderId, List<RefundRequest.LineItem> refundItemRequests) {
        var fulfillmentOrderInfo = getFulfillmentInfo(orderId);
    }

    private List<FFORecord> getFulfillmentInfo(OrderId orderId) {
        var fulfillmentOrders = fulfillmentOrderDao.getByOrderId(orderId.getStoreId(), orderId.getId());
        var fulfillmentOrderIds = fulfillmentOrders.stream()
                .map(FulfillmentOrderDto::getId)
                .distinct().toList();
        var fulfillmentLineItems = fulfillmentOrderLineItemDao.getByFulfillmentOrderIds(orderId.getStoreId(), fulfillmentOrderIds);
        return fulfillmentOrders.stream()
                .map(ffo -> {
                    var ffoId = ffo.getId();
                    var ffoLines = fulfillmentLineItems.stream()
                            .filter(line -> line.getFulfillmentOrderId() == ffoId)
                            .toList();
                    return new FFORecord(ffo, ffoLines);
                })
                .toList();
    }

    private void validateRefundItem(List<LineItem> lineItems, List<RefundCalculateResponse.LineItem> refundableLineItems, List<RefundRequest.LineItem> refundItemRequests) {
        var requestedRefundItemMap = reduceToTotalRefundQuantityLineItem(refundItemRequests);
        for (var refundItemEntry : requestedRefundItemMap.entrySet()) {
            var noneExistLine = refundableLineItems.stream().noneMatch(line -> line.getLineItemId() == refundItemEntry.getKey());
            if (noneExistLine) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        "cannot be blank anh must belong to the order being refunded"
                );
            }
            var refundableLineItem = refundableLineItems.stream()
                    .filter(line -> line.getLineItemId() == refundItemEntry.getKey())
                    .findFirst()
                    .orElse(null);
            var validationModel = refundItemEntry.getValue();
            assert refundableLineItem != null;
            if (refundableLineItem.getMaximumRefundableQuantity() < validationModel.getQuantity()) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        ""
                );
            }
            if (validationModel.getRemoveQuantity() > refundableLineItem.getLineItem().getFulfillableQuantity()) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        ""
                );
            }
        }
    }

    private Map<Integer, RefundItemValidationModel> reduceToTotalRefundQuantityLineItem(List<RefundRequest.LineItem> refundItemRequests) {
        Map<Integer, RefundItemValidationModel> modelMap = new HashMap<>();
        for (var refundRequest : refundItemRequests) {
            var model = modelMap.get(refundRequest.getLineItemId());
            if (model != null) {
                model.add(refundRequest);
                continue;
            }
            model = new RefundItemValidationModel(refundRequest);
            modelMap.put(refundRequest.getLineItemId(), model);
        }
        return modelMap;
    }

    private void forceRestockType(Order order, List<RefundCalculateResponse.LineItem> refundableLineItems, List<RefundRequest.LineItem> refundItemRequests) {
        forceDefaultRestockType(refundItemRequests);
        forceRestockTypeIfProductNotExists(order, refundItemRequests, refundableLineItems);
    }

    private void forceRestockTypeIfProductNotExists(Order order, List<RefundRequest.LineItem> refundItemRequests, List<RefundCalculateResponse.LineItem> refundableLineItems) {
        var lineItemMap = refundableLineItems.stream()
                .collect(Collectors.toMap(
                        RefundCalculateResponse.LineItem::getLineItemId,
                        RefundCalculateResponse.LineItem::getLineItem,
                        (line1, line2) -> line1));
        var existVariantOfLines = new ArrayList<Pair<RefundRequest.LineItem, Integer>>();

        for (var refundRequest : refundItemRequests) {
            var lineItemId = refundRequest.getLineItemId();
            var lineItem = lineItemMap.get(lineItemId);
            if (lineItem == null) continue; // chưa xử lý ở đây
            if (lineItem.getVariantInfo() != null && lineItem.getVariantInfo().getVariantId() != null) {
                existVariantOfLines.add(Pair.of(refundRequest, lineItem.getVariantInfo().getVariantId()));
            } else {
                // line custom thì set restock_type = no_restock
                refundRequest.setRestockType(RefundLineItem.RestockType.no_restock);
            }
        }

        if (CollectionUtils.isNotEmpty(existVariantOfLines)) {
            var variantIds = existVariantOfLines.stream()
                    .map(Pair::getValue)
                    .filter(NumberUtils::isPositive)
                    .distinct().toList();
            var variants = productDao.findVariantByListIds(order.getId().getStoreId(), variantIds);
            var variantIdExists = variants.stream().map(VariantDto::getId).distinct().toList();
            existVariantOfLines.stream()
                    .filter(pair -> !variantIdExists.contains(pair.getValue()))
                    .forEach(pair -> {
                        var refundRequest = pair.getKey();
                        refundRequest.setRestockType(RefundLineItem.RestockType.no_restock);
                    });
        }
    }

    // setup or build lại các field trong refund_requests
    private void forceDefaultRestockType(List<RefundRequest.LineItem> refundItemRequests) {
        if (CollectionUtils.isEmpty(refundItemRequests)) return;

        for (var refundLine : refundItemRequests) {
            if (refundLine.getRestockType() == null) {
                refundLine.setRestockType(RefundLineItem.RestockType.no_restock);
            }
            switch (refundLine.getRestockType()) {
                case _return -> refundLine.setRemoval(false);
                case cancel -> refundLine.setRemoval(true);
            }
        }
    }

    private List<RefundCalculateResponse.LineItem> getRefundableLineItems(Order order) {
        Map<Integer, Integer> refundedLineMap = new HashMap<>();
        List<RefundCalculateResponse.LineItem> refundableLineItems = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(order.getRefunds())) {
            order.getRefunds().forEach(refund -> {
                var refundedLineItems = refund.getRefundLineItems();
                for (var refundLine : refundedLineItems) {
                    refundedLineMap.compute(
                            refundLine.getLineItemId(),
                            (lineItemId, existedQuantity) -> {
                                if (existedQuantity == null) return refundLine.getQuantity();
                                return existedQuantity + refundLine.getQuantity();
                            });
                }
            });
        }

        for (var lineItem : order.getLineItems()) {
            if (LineItem.FulfillmentStatus.restocked == lineItem.getFulfillmentStatus()) {
                continue;
            }

            var lineItemId = lineItem.getId();
            var refundedQuantity = refundedLineMap.getOrDefault(lineItemId, 0);
            if (lineItem.getQuantity() > refundedQuantity) {
                var refundLineItem = new RefundCalculateResponse.LineItem()
                        .setLineItem(lineItem)
                        .setLineItemId(lineItemId)
                        .setPrice(lineItem.getDiscountUnitPrice())
                        .setOriginalPrice(lineItem.getPrice())
                        .setMaximumRefundableQuantity(lineItem.getQuantity() - refundedQuantity);
                refundableLineItems.add(refundLineItem);
            }
        }

        return refundableLineItems;
    }

    private <T> List<T> safeSelect(List<T> requests, Predicate<T> condition) {
        if (CollectionUtils.isEmpty(requests)) return Collections.emptyList();
        return requests.stream().filter(condition).toList();
    }

    private RefundCalculateResponse.Shipping suggestRefundShipping(Order order, RefundRequest.Shipping request) {
        var refundSuggestion = new RefundCalculateResponse.Shipping();

        var totalShipping = BigDecimal.ZERO;
        var refundedShipping = BigDecimal.ZERO;
        var totalTax = BigDecimal.ZERO;
        var refundedTax = BigDecimal.ZERO;

        if (CollectionUtils.isNotEmpty(order.getShippingLines())) {
            for (var shippingLine : order.getShippingLines()) {
                totalShipping = totalShipping.add(shippingLine.getPrice());
                if (CollectionUtils.isEmpty(shippingLine.getTaxLines())) continue;
                totalTax = totalTax.add(
                        shippingLine.getTaxLines().stream()
                                .map(TaxLine::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        }

        if (CollectionUtils.isNotEmpty(order.getRefunds())) {
            var shippingRefunds = order.getRefunds().stream()
                    .filter(refund -> CollectionUtils.isNotEmpty(refund.getOrderAdjustments()))
                    .flatMap(refund -> refund.getOrderAdjustments().stream())
                    .filter(oa -> oa.getRefundKind() == OrderAdjustment.RefundKind.shipping_refund)
                    .toList();
            if (CollectionUtils.isNotEmpty(shippingRefunds)) {
                for (var shippingRefund : shippingRefunds) {
                    refundedShipping = refundedShipping.add(shippingRefund.getAmount());
                    if (order.isTaxIncluded()) {
                        refundedTax = refundedTax.add(shippingRefund.getTaxAmount());
                    }
                }
            }
        }

        var maxRefundableAmount = totalShipping.subtract(refundedShipping);
        refundSuggestion.setMaximumRefundable(maxRefundableAmount);

        if (request != null) {
            var roundingAccuracy = order.getMoneyInfo().getCurrency().getDefaultFractionDigits();
            if (NumberUtils.isPositive(request.getAmount())) {
                refundSuggestion.setAmount(request.getAmount().setScale(roundingAccuracy, RoundingMode.FLOOR));
            } else if (BooleanUtils.isTrue(request.getFullRefund())) {
                refundSuggestion.setAmount(refundSuggestion.getMaximumRefundable());
            }

            var compareAmountResult = refundSuggestion.getAmount().compareTo(refundSuggestion.getMaximumRefundable());

            if (compareAmountResult > 0) {
                throw new ConstrainViolationException(
                        "refund_amount",
                        "must be less than or equal to maximum_refundable"
                );
            }

            BigDecimal suggestTaxAmount;
            if (compareAmountResult == 0) {
                suggestTaxAmount = totalTax.subtract(refundedTax)
                        .setScale(roundingAccuracy, RoundingMode.FLOOR);
            } else {
                suggestTaxAmount = refundSuggestion.getAmount()
                        .multiply(totalTax)
                        .divide(totalShipping, roundingAccuracy, RoundingMode.FLOOR);
            }
            refundSuggestion.setTax(suggestTaxAmount);
        }

        return refundSuggestion;
    }

    @Getter
    private static class RestockItemContext {
        private int remaining;
        private int cancelable;
        private int returnable;

        public RestockItemContext(int remaining, int cancelable, int returnable) {
            this.remaining = remaining;
            this.cancelable = cancelable;
            this.returnable = returnable;
        }

        public int reduce(int quantity, boolean removal) {
            if (removal) {
                if (this.cancelable == 0) return 0;
                int toCancel = Math.min(quantity, cancelable);
                this.cancelable -= toCancel;
                this.remaining -= toCancel;
                return toCancel;
            } else {
                if (this.returnable == 0) return 0;
                int toReturn = Math.min(returnable, quantity);
                this.returnable -= toReturn;
                this.remaining -= toReturn;
                return toReturn;
            }
        }
    }

    @Getter
    private static class RefundItemContext {
        private int refunded; // sô lượng đã trả về
        private int removed; // số lượng mà cancel
        private int returned; // số lượng đã được hoàn lại kho

        public RefundItemContext(RefundLineItem refundLine) {
            this.add(refundLine);
        }

        public void add(RefundLineItem refundLine) {
            this.refunded += refundLine.getQuantity();
            switch (refundLine.getRestockType()) {
                case cancel -> this.removed += refundLine.getQuantity();
                case _return -> this.returned += refundLine.getQuantity();
                case no_restock -> {
                    if (refundLine.isRemoval()) {
                        this.removed += refundLine.getQuantity();
                    } else {
                        this.returned += refundLine.getQuantity();
                    }
                }
            }
        }
    }

    private record FFORecord(FulfillmentOrderDto fulfillmentOrder,
                             List<FulfillmentOrderLineItemDto> fulfillmentOrderLineItems) {
    }

    @Getter
    private static class RefundItemValidationModel {
        private int quantity; // tổng quantity refund của line_item đó
        private int removeQuantity; // tổng quantity, của line có removal = true

        public RefundItemValidationModel(RefundRequest.LineItem refundRequest) {
            this.add(refundRequest);
        }

        public void add(RefundRequest.LineItem refundRequest) {
            this.quantity += refundRequest.getQuantity();
            if (refundRequest.isRemoval()) {
                this.removeQuantity += refundRequest.getQuantity();
            }
        }
    }

    record RefundItemResult(List<RefundCalculateResponse.LineItem> refundableLineItems,
                            List<RefundCalculateResponse.LineItem> refundLineItems) {
        static RefundItemResult EMPTY() {
            return new RefundItemResult(List.of(), List.of());
        }
    }
}
