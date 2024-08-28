package org.example.order.order.application.service.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.model.order.request.LocationFilter;
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
import org.example.order.order.infrastructure.data.dto.Location;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RefundCalculationService {

    private final ProductDao productDao;
    private final FulfillmentOrderDao fulfillmentOrderDao;
    private final FulfillmentOrderLineItemDao fulfillmentOrderLineItemDao;

    public RefundCalculateResponse calculateRefund(Order order, RefundRequest refundRequest) {
        var shipping = suggestRefundShipping(order, refundRequest.getShipping());
        var refundLines = suggestRefundLines(order, refundRequest);
        return null;
    }

    private RefundCalculateResponse.Shipping suggestRefundShipping(Order order, RefundRequest.Shipping request) {
        var refundSuggestion = new RefundCalculateResponse.Shipping();

        var totalShipping = BigDecimal.ZERO;
        var refundedShipping = BigDecimal.ZERO;
        var totalTax = BigDecimal.ZERO;
        var refundedTax = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(order.getShippingLines())) {
            for (var shippingLine : order.getShippingLines()) {
                totalShipping = totalShipping.add(shippingLine.getPrice());
                if (!CollectionUtils.isEmpty(shippingLine.getTaxLines())) {
                    totalTax = totalTax.add(shippingLine.getTaxLines().stream()
                            .map(TaxLine::getPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                }
            }
            if (!CollectionUtils.isEmpty(order.getRefunds())) {
                var refundedShippingLines = order.getRefunds().stream()
                        .filter(refund -> !CollectionUtils.isEmpty(refund.getOrderAdjustments()))
                        .flatMap(refund -> refund.getOrderAdjustments().stream())
                        .filter(oa -> oa.getRefundKind() == OrderAdjustment.RefundKind.shipping_refund)
                        .toList();
                for (var refundedShippingLine : refundedShippingLines) {
                    refundedShipping = refundedShipping.add(refundedShippingLine.getAmount());
                    if (order.isTaxIncluded()) {
                        refundedShipping = refundedShipping.add(refundedShippingLine.getTaxAmount());
                    }
                    refundedTax = refundedTax.add(refundedShippingLine.getTaxAmount());
                }
            }
            var maxRefundableAmount = totalShipping.subtract(refundedShipping);
            refundSuggestion.setMaximumRefundable(maxRefundableAmount);
        }
        if (request != null) {
            // ưu tiên amount trước
            var roundingAccuracy = order.getMoneyInfo().getCurrency().getDefaultFractionDigits();
            if (NumberUtils.isPositive(request.getAmount())) {
                refundSuggestion.setAmount(request.getAmount().setScale(roundingAccuracy, RoundingMode.FLOOR)); // làm tròn xuống
            } else if (BooleanUtils.isTrue(request.getFullRefund())) {
                refundSuggestion.setAmount(refundSuggestion.getMaximumRefundable());
            }
            var amountCompareResult = refundSuggestion.getAmount().compareTo(refundSuggestion.getMaximumRefundable());
            if (amountCompareResult > 0) {
                throw new ConstrainViolationException(
                        "shipping_amount",
                        "must be less than or equal to max refundable of order"
                );
            }
            if (NumberUtils.isPositive(totalTax)) {
                if (amountCompareResult == 0) {
                    refundSuggestion.setTax(totalTax.subtract(refundedTax));
                } else {
                    var tax = refundSuggestion.getAmount()
                            .multiply(totalTax)
                            .divide(totalShipping, roundingAccuracy, RoundingMode.FLOOR);
                    refundSuggestion.setTax(tax);
                }
            }
        }

        return refundSuggestion
                .setAmount(refundSuggestion.getAmount().stripTrailingZeros())
                .setTax(refundSuggestion.getTax().stripTrailingZeros())
                .setMaximumRefundable(refundSuggestion.getMaximumRefundable().stripTrailingZeros());
    }

    private <T> List<T> safeSelect(List<T> list, Predicate<T> condition) {
        if (list == null || list.isEmpty()) return List.of();
        return list.stream().filter(condition).toList();
    }

    private RefundItemResult suggestRefundLines(Order order, RefundRequest request) {
        var requestLines = safeSelect(request.getRefundLineItems(), line -> line.getQuantity() > 0);
        if (requestLines.isEmpty()) {
            if (request.getOption().isCreate()) {
                var refundableLineItems = getRefundableLineItems(order);
                return RefundItemResult.of(refundableLineItems, List.of());
            }
            return RefundItemResult.EMPTY;
        }

        return suggestRefundLines(order, requestLines, request.getOption());
    }

    private RefundItemResult suggestRefundLines(
            Order order,
            List<RefundRequest.LineItem> requestLines,
            RefundRequest.Option refundOption
    ) {
        var refundableLineItems = getRefundableLineItems(order);

        forceRestockType(order.getId(), refundableLineItems, requestLines, refundOption);

        validateRefundItem(order.getLineItems(), refundableLineItems, requestLines);

        validateLocation(order.getId(), requestLines, refundOption);

        if (!refundOption.isLegacy() || refundOption.isCreate()) {
            requestLines = chooseRefundTypeForLineItem(order, refundableLineItems, requestLines);
        }

        var refundLineItems = !checkOrderWithNonAllocateDiscount(order)
                ? calculateRefundLineItemInfo(order, refundableLineItems, requestLines)
                : null;
        return RefundItemResult.of(refundableLineItems, refundLineItems);
    }

    private List<RefundCalculateResponse.LineItem> calculateRefundLineItemInfo(
            Order order,
            List<RefundCalculateResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> requestLineItems
    ) {
        var calculationResultList = new ArrayList<RefundCalculateResponse.LineItem>();
        Map<Integer, Long> processedLineQuantityCache = new HashMap<>();
        for (var requestLine : requestLineItems) {
            var refundableLineItem = refundableLineItems.stream()
                    .filter(line -> line.getLineItemId() == requestLine.getLineItemId())
                    .findFirst().get();

            var lineItem = refundableLineItem.getLineItem();
            var suggestQuantity = Math.min(requestLine.getQuantity(), refundableLineItem.getMaximumRefundableQuantity());
            var refundedQuantity = lineItem.getQuantity() - refundableLineItem.getMaximumRefundableQuantity();

            long localRefundedQuantity = processedLineQuantityCache.getOrDefault(lineItem.getId(), 0L);
            refundedQuantity += Math.toIntExact(localRefundedQuantity);

            var lineItemDiscountDetails = categorizeLineItemDiscount(lineItem, order);
            var totalProductDiscount = lineItemDiscountDetails.getLeft();
            var totalCartDiscount = lineItemDiscountDetails.getRight();

            var totalTax = lineItem.getTaxLines().stream()
                    .map(TaxLine::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var calculateRefundLine = refundableLineItem.copy();
            calculationResultList.add(calculateRefundLine);

            calculateRefundLine
                    .setLocationId(requestLine.getLocationId())
                    .setRestockType(requestLine.getRestockType())
                    .setRemoval(requestLine.isRemoval());

            var refundAllRemaining = suggestQuantity == refundableLineItem.getMaximumRefundableQuantity();
            if (refundAllRemaining && refundedQuantity == 0) {
                // refund lần đầu tiên và refund all
                this.suggestRefundPrice(
                        calculateRefundLine,
                        lineItem.getPrice(),
                        totalProductDiscount,
                        totalCartDiscount,
                        totalTax,
                        lineItem.getQuantity()
                );
            } else {
                var roundingAccuracy = order.getMoneyInfo().getCurrency().getDefaultFractionDigits();
                var suggestTotalTax = suggestRefundAmount(
                        totalTax, roundingAccuracy,
                        lineItem.getQuantity(), refundedQuantity,
                        suggestQuantity
                );
            }
        }
        return null;
    }

    private BigDecimal suggestRefundAmount(
            BigDecimal amount,
            int fractionDigits, int totalQuantity,
            int refundedQuantity, int suggestQuantity
    ) {
        return suggestRefundAmount(
                amount, fractionDigits,
                totalQuantity, refundedQuantity,
                suggestQuantity, RoundingStyle.last_n
        );
    }

    public static BigDecimal suggestRefundAmount(
            BigDecimal amount, int fractionDigits,
            int totalQuantity, int refundedQuantity,
            int suggestQuantity, RoundingStyle roundingStyle
    ) {
        long totalAmountL = fractionDigits == 0
                ? amount.longValue()
                : amount.movePointRight(fractionDigits).longValue();
        long subtotalAmountL = subtotalWithRoundingStyle(totalAmountL, totalQuantity, refundedQuantity, suggestQuantity, roundingStyle);
        if (fractionDigits == 0) return BigDecimal.valueOf(subtotalAmountL);
        return BigDecimal.valueOf(subtotalAmountL).movePointLeft(fractionDigits);
    }

    public static long subtotalWithRoundingStyle(
            long amount, int quantity,
            int refundedQuantity, int suggestQuantity,
            RoundingStyle rs
    ) {
        var remain = amount % quantity;
        if (remain == 0) return (amount / quantity) * suggestQuantity;
        var rounding = switch (rs) {
            case last_n -> {
                var delta = quantity - (int) remain;
                yield roundingFromLastItem(refundedQuantity, suggestQuantity, delta);
            }
            case first_n -> {
                var delta = (int) remain;
                yield roundingFromFirstItem(refundedQuantity, suggestQuantity, delta);
            }
        };
        return (amount / quantity) * suggestQuantity + rounding;
    }

    public static int roundingFromLastItem(int a, int b, int delta) {
        if (a + b <= delta) return 0;
        if (a >= delta) return b;
        return b + a - delta;
    }

    public static int roundingFromFirstItem(int a, int b, int delta) {
        if (a + b <= delta) return b;
        if (a >= delta) return 0;
        return delta - a;
    }

    public enum RoundingStyle {
        last_n, first_n
    }

    private void suggestRefundPrice(
            RefundCalculateResponse.LineItem calculateRefundLine,
            BigDecimal price,
            BigDecimal totalProductDiscount,
            BigDecimal totalCartDiscount,
            BigDecimal totalTax,
            int suggestQuantity
    ) {
        var quantity = BigDecimal.valueOf(suggestQuantity);
        var discountSubtotal = price.multiply(quantity).subtract(totalProductDiscount);
        var discountedUnitPrice = discountSubtotal.divide(quantity, RoundingMode.FLOOR);
        var subtotal = discountSubtotal.subtract(totalCartDiscount);

        calculateRefundLine
                .setSubtotal(subtotal.stripTrailingZeros())
                .setTotalTax(totalTax.stripTrailingZeros())
                .setTotalCartDiscount(totalCartDiscount.stripTrailingZeros())
                .setDiscountedPrice(discountedUnitPrice.stripTrailingZeros())
                .setDiscountedSubtotal(discountSubtotal.stripTrailingZeros());
    }

    private Pair<BigDecimal, BigDecimal> categorizeLineItemDiscount(LineItem lineItem, Order order) {
        if (!CollectionUtils.isEmpty(lineItem.getDiscountAllocations())) {
            BigDecimal productDiscount = BigDecimal.ZERO;
            BigDecimal cartDiscount = BigDecimal.ZERO;
            for (var allocation : lineItem.getDiscountAllocations()) {
                if (filterProductDiscount(allocation, order)) {
                    productDiscount = productDiscount.add(allocation.getAmount());
                } else {
                    cartDiscount = cartDiscount.add(allocation.getAmount());
                }
            }
            return Pair.of(productDiscount, cartDiscount);
        }
        return Pair.of(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private boolean filterProductDiscount(DiscountAllocation allocation, Order order) {
        var application = order.getDiscountApplications().get(allocation.getApplicationIndex());
        return application.getRuleType() == DiscountApplication.RuleType.product;
    }

    private boolean checkOrderWithNonAllocateDiscount(Order order) {
        return !CollectionUtils.isEmpty(order.getDiscountCodes())
                && CollectionUtils.isEmpty(order.getDiscountApplications());
    }

    private List<RefundRequest.LineItem> chooseRefundTypeForLineItem(
            Order order,
            List<RefundCalculateResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> refundItemRequests
    ) {
        var refundedDetails = getRefundedQuantityDetail(order.getRefunds());
        var restockState = refundableLineItems.stream().collect(Collectors.toMap(
                RefundCalculateResponse.LineItem::getLineItemId,
                item -> {
                    int removableQuantity = item.getLineItem().getFulfillableQuantity();
                    var refunded = refundedDetails.get(item.getLineItemId());
                    if (refunded != null && refunded.getLegacy() > 0) {
                        removableQuantity = Math.min(
                                removableQuantity,
                                item.getMaximumRefundableQuantity()
                        );
                    }
                    return new RestockItemContext(
                            item.getMaximumRefundableQuantity(),
                            removableQuantity,
                            item.getMaximumRefundableQuantity() - removableQuantity
                    );
                }));

        var processedRequests = new ArrayList<RefundRequest.LineItem>();
        for (var refundItemRequest : refundItemRequests) {
            var state = restockState.get(refundItemRequest.getLineItemId());
            switch (refundItemRequest.getRestockType()) {
                case no_restock -> {
                    var splitLineItems = splitLineItem(refundItemRequest, state);
                    processedRequests.addAll(splitLineItems);
                }
                case _return -> {
                    refundItemRequest.setRemoval(false);
                    var splitLineItems = splitLineItem(refundItemRequest, state);
                    processedRequests.addAll(splitLineItems);
                }
                case cancel -> {
                    refundItemRequest.setRemoval(true);
                    state.reduce(refundItemRequest.getQuantity(), true);
                    processedRequests.add(refundItemRequest);
                }
            }
        }
        return processedRequests;
    }

    private List<RefundRequest.LineItem> splitLineItem(
            RefundRequest.LineItem original,
            RestockItemContext state
    ) {
        var refundLineItems = new ArrayList<RefundRequest.LineItem>(2);
        var remaining = state.reduce(original.getQuantity(), original.isRemoval());
        if (remaining > 0) {
            var returnRequest = original.toBuilder()
                    .quantity(remaining)
                    .removal(original.isRemoval())
                    .build();
            refundLineItems.add(returnRequest);
        }
        if (remaining < original.getQuantity()) {
            var restockType = switch (original.getRestockType()) {
                case no_restock -> RefundLineItem.RestockType.no_restock;
                case cancel -> RefundLineItem.RestockType._return;
                case _return -> RefundLineItem.RestockType.cancel;
                case legacy_restock -> throw new IllegalArgumentException();
            };
            var splitQuantity = state.reduce(original.getQuantity() - remaining, !original.isRemoval());
            var cancelRequest = original.toBuilder()
                    .restockType(restockType)
                    .quantity(splitQuantity)
                    .removal(!original.isRemoval())
                    .build();
            refundLineItems.add(cancelRequest);
        }
        return refundLineItems;
    }

    @Getter
    static class RestockItemContext {
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
                var toCancel = Math.min(quantity, cancelable);
                this.remaining -= toCancel;
                this.cancelable -= toCancel;
                return toCancel;
            } else {
                if (this.returnable == 0) return 0;
                var toReturn = Math.min(quantity, returnable);
                this.remaining -= toReturn;
                this.returnable -= toReturn;
                return toReturn;
            }
        }
    }

    private Map<Integer, RefundLineItemContext> getRefundedQuantityDetail(Set<Refund> refunds) {
        var records = new HashMap<Integer, RefundLineItemContext>();
        for (var refund : refunds) {
            if (CollectionUtils.isEmpty(refund.getRefundLineItems())) {
                continue;
            }
            for (var refundLineItem : refund.getRefundLineItems()) {
                var record = records.get(refundLineItem.getLineItemId());
                if (record == null) {
                    record = new RefundLineItemContext(refundLineItem);
                    records.put(refundLineItem.getLineItemId(), record);
                    continue;
                }
                record.add(refundLineItem);
            }
        }
        return records;
    }

    @Getter
    static class RefundLineItemContext {
        private int refunded;
        private int returned;
        private int removed;
        private int legacy;

        public RefundLineItemContext(RefundLineItem refundLineItem) {
            this.add(refundLineItem);
        }

        public void add(RefundLineItem refundLineItem) {
            refunded += refundLineItem.getQuantity();
            switch (refundLineItem.getRestockType()) {
                case cancel -> removed += refundLineItem.getQuantity();
                case _return -> returned += refundLineItem.getQuantity();
                case no_restock -> {
                    if (refundLineItem.isRemoval()) {
                        removed += refundLineItem.getQuantity();
                    } else {
                        returned += refundLineItem.getQuantity();
                    }
                }
                case legacy_restock -> legacy += refundLineItem.getQuantity();
            }
        }
    }

    private void forceRestockType(
            OrderId id,
            List<RefundCalculateResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> requestLines,
            RefundRequest.Option refundOption
    ) {
        if (refundableLineItems.isEmpty()) return;
        forceDefaultRestockType(requestLines, refundOption);
        forceRestockTypeIfProductNotExists(id.getStoreId(), requestLines, refundableLineItems);
    }

    private void forceDefaultRestockType(List<RefundRequest.LineItem> requestLines, RefundRequest.Option refundOption) {
        if (CollectionUtils.isEmpty(requestLines)) {
            return;
        }
        if (refundOption.isLegacy()) {
            for (var refundLineItem : requestLines) {
                refundLineItem.setRemoval(false);
                refundLineItem.setRestockType(refundOption.isRestock()
                        ? RefundLineItem.RestockType._return
                        : RefundLineItem.RestockType.no_restock);
            }
            return;
        }
        for (var refundLineItem : requestLines) {
            if (refundLineItem.getRestockType() == null) {
                refundLineItem.setRestockType(RefundLineItem.RestockType.no_restock);
            }
            switch (refundLineItem.getRestockType()) {
                case _return -> refundLineItem.setRemoval(false);
                case cancel -> refundLineItem.setRemoval(true);
            }
        }
    }

    private void forceRestockTypeIfProductNotExists(
            int storeId,
            List<RefundRequest.LineItem> requestLines,
            List<RefundCalculateResponse.LineItem> refundableLineItems
    ) {
        var lineItemMap = refundableLineItems.stream()
                .map(RefundCalculateResponse.LineItem::getLineItem)
                .collect(Collectors.toMap(LineItem::getId, lineItem -> lineItem));

        var variantIds = new ArrayList<Integer>();
        var requestLineCheckProducts = new ArrayList<Pair<RefundRequest.LineItem, Integer>>();

        for (var requestLine : requestLines) {
            if (requestLine.getRestockType() == RefundLineItem.RestockType.no_restock) continue;
            var lineItem = lineItemMap.get(requestLine.getLineItemId());
            if (lineItem == null) continue;
            if (lineItem.getVariantInfo().isProductExisted() && lineItem.getVariantInfo().getVariantId() != null) {
                var variantId = lineItem.getVariantInfo().getVariantId();
                variantIds.add(variantId);
                requestLineCheckProducts.add(Pair.of(requestLine, variantId));
            } else {
                requestLine.setRestockType(RefundLineItem.RestockType.no_restock);
            }
        }
        if (!variantIds.isEmpty()) {
            var variants = productDao.findVariantByListIds(storeId, variantIds);
            if (variants.isEmpty()) {
                requestLineCheckProducts.forEach(pair -> {
                    var requestLine = pair.getLeft();
                    requestLine.setRestockType(RefundLineItem.RestockType.no_restock);
                });
            } else {
                for (var pair : requestLineCheckProducts) {
                    int variantId = pair.getRight();
                    var variant = variants.stream().filter(v -> v.getId() == variantId).findFirst().orElse(null);
                    if (variant == null) {
                        var requestLine = pair.getLeft();
                        requestLine.setRestockType(RefundLineItem.RestockType.no_restock);
                    }
                }
            }
        }
    }

    private void validateRefundItem(
            List<LineItem> lineItems,
            List<RefundCalculateResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> requestLines
    ) {
        var requestedRefundItemMap = reduceToTotalRefundQuantityLineItem(requestLines);
        for (var refundItemEntry : requestedRefundItemMap.entrySet()) {
            int lineItemId = refundItemEntry.getKey();
            var noneExistLine = lineItems.stream().noneMatch(line -> line.getId() == lineItemId);
            if (noneExistLine) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        "cannot be blank and must belong to the order being refunded"
                );
            }
            var refundableLineItem = refundableLineItems.stream()
                    .filter(line -> line.getLineItemId() == lineItemId)
                    .findFirst().orElse(null);
            var validationModel = refundItemEntry.getValue();
            if (refundableLineItem == null || validationModel.getQuantity() > refundableLineItem.getMaximumRefundableQuantity()) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        "cannot refund more item than were purchased"
                );
            }
            if (validationModel.getRemoveQuantity() > refundableLineItem.getLineItem().getFulfillableQuantity()) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        "cannot remove more than the fulfillable quantity"
                );
            }
            var restockTypeSupported = requestLines.stream()
                    .noneMatch(request -> RefundLineItem.RestockType.legacy_restock == request.getRestockType());
            if (!restockTypeSupported) {
                throw new ConstrainViolationException(
                        "restock_type",
                        "is not included in supported list"
                );
            }
        }
    }

    private void validateLocation(
            OrderId orderId,
            List<RefundRequest.LineItem> refundItemRequests,
            RefundRequest.Option option
    ) {
        if (option.isLegacy()) {
            refundItemRequests.forEach(request -> request.setLocationId(null));
            var locationFilter = LocationFilter.builder().defaultLocation(true).build();
            var locations = new ArrayList<Location>();
            if (locations.isEmpty()) {
                throw new ConstrainViolationException("base", "require default location");
            }
            var defaultLocation = locations.get(0);
            refundItemRequests.forEach(request -> request.setLocationId(defaultLocation.getId()));
            return;
        }

        var fulfillmentOrders = findFulfillmentOrderByOrderId(orderId).stream()
                .sorted(Comparator.comparingLong(record -> record.ffo.getId()))
                .toList();
        var ffoLocationIds = fulfillmentOrders.stream()
                .map(rc -> rc.ffo().getAssignedLocationId())
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();
        var inputLocationIds = refundItemRequests.stream()
                .map(RefundRequest.LineItem::getLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var filterLocationIds = Stream.concat(ffoLocationIds.stream(), inputLocationIds.stream()).distinct().toList();
        if (filterLocationIds.isEmpty()) {
            return;
        }
        var locationFilter = LocationFilter.builder().locationIds(filterLocationIds).limit(250).build();
        var locations = new ArrayList<Location>();
        var locationMap = locations.stream().collect(Collectors.toMap(Location::getId, Function.identity()));

        if (!inputLocationIds.isEmpty()) {
            for (var inputLocationId : inputLocationIds) {
                if (locationMap.containsKey(inputLocationId)) continue;
                throw new ConstrainViolationException("base", "require valid location");
            }
        }
        for (var refundItemRequest : refundItemRequests) {
            if (option.isCreate() && !option.isLegacy()) {
                if (refundItemRequest.isRestock() && refundItemRequest.getLocationId() == null) {
                    throw new ConstrainViolationException("base", "require location with inventory management");
                }
            }

            Long defaultLocationId;
            if (refundItemRequest.getLocationId() == null) {
                int lineItemId = refundItemRequest.getLineItemId();
                defaultLocationId = fulfillmentOrders.stream()
                        .filter(rc -> rc.ffoItems.stream().anyMatch(ffoLine -> ffoLine.getLineItemId() == lineItemId))
                        .map(rc -> rc.ffo().getAssignedLocationId())
                        .findFirst().orElse(null);
            } else {
                defaultLocationId = refundItemRequest.getLocationId();
            }

            var restockLocation = filterDefaultLocation(locations, defaultLocationId);
            if (restockLocation == null) {
                throw new ConstrainViolationException("base", "require location with inventory management");
            }
            refundItemRequest.setLocationId(restockLocation.getId());
            if (!restockLocation.isInventoryManagement()) {
                refundItemRequest.setRestockType(RefundLineItem.RestockType.no_restock);
            }
        }
    }

    private Location filterDefaultLocation(ArrayList<Location> locations, Long locationId) {
        if (NumberUtils.isPositive(locationId)) {
            var defaultLocation = locations.stream()
                    .filter(l -> l.getId() == locationId)
                    .findFirst().orElse(null);
            if (defaultLocation != null) {
                return defaultLocation;
            }
        }
        return locations.stream()
                .filter(Location::isDefaultLocation)
                .findFirst()
                .orElse(locations.isEmpty() ? null : locations.get(0));
    }

    private List<FFORecord> findFulfillmentOrderByOrderId(OrderId orderId) {
        var ffoOrders = fulfillmentOrderDao.getByOrderId(orderId.getStoreId(), orderId.getId());
        if (CollectionUtils.isEmpty(ffoOrders)) {
            return List.of();
        }
        var ffoOrderIds = ffoOrders.stream().map(FulfillmentOrderDto::getId).distinct().toList();
        var ffoItems = fulfillmentOrderLineItemDao.getByFulfillmentOrderIds(orderId.getStoreId(), ffoOrderIds);

        var results = new ArrayList<FFORecord>();
        for (var ffo : ffoOrders) {
            var ffoLineItems = ffoItems.stream()
                    .filter(fli -> fli.getFulfillmentOrderId() == ffo.getId())
                    .toList();
            results.add(FFORecord.of(ffo, ffoLineItems));
        }
        return Collections.unmodifiableList(results);
    }

    record FFORecord(
            FulfillmentOrderDto ffo,
            List<FulfillmentOrderLineItemDto> ffoItems
    ) {
        public static FFORecord of(
                FulfillmentOrderDto ffo,
                List<FulfillmentOrderLineItemDto> ffoItems
        ) {
            return new FFORecord(ffo, ffoItems);
        }
    }

    private Map<Integer, RefundItemValidationModel> reduceToTotalRefundQuantityLineItem(List<RefundRequest.LineItem> requestLines) {
        var validationEntries = new LinkedHashMap<Integer, RefundItemValidationModel>();
        for (var requestLine : requestLines) {
            var refundItemModel = validationEntries.get(requestLine.getLineItemId());
            if (refundItemModel == null) {
                refundItemModel = new RefundItemValidationModel(requestLine);
                validationEntries.put(requestLine.getLineItemId(), refundItemModel);
                continue;
            }
            refundItemModel.add(requestLine);
        }
        return validationEntries;
    }

    @Getter
    static class RefundItemValidationModel {
        private int quantity;
        private int removeQuantity;

        public RefundItemValidationModel(RefundRequest.LineItem requestLine) {
            this.add(requestLine);
        }

        public void add(RefundRequest.LineItem requestLine) {
            this.quantity += requestLine.getQuantity();
            if (requestLine.isRemoval()) {
                removeQuantity += requestLine.getQuantity();
            }
        }
    }

    private List<RefundCalculateResponse.LineItem> getRefundableLineItems(Order order) {
        var refundableLineItems = new ArrayList<RefundCalculateResponse.LineItem>();
        var refundedLineItems = order.getRefunds().stream()
                .filter(refund -> !CollectionUtils.isEmpty(refund.getRefundLineItems()))
                .flatMap(refund -> refund.getRefundLineItems().stream())
                .toList();
        for (var lineItem : order.getLineItems()) {
            if (lineItem.getFulfillmentStatus() == LineItem.FulfillmentStatus.restocked) {
                continue;
            }
            int refundedQuantity = refundedLineItems.stream()
                    .filter(line -> line.getLineItemId() == lineItem.getId())
                    .mapToInt(RefundLineItem::getQuantity)
                    .sum();
            if (lineItem.getQuantity() > refundedQuantity) {
                // tất cả price đều tính trên `1 quantity
                var refundLineItem = new RefundCalculateResponse.LineItem()
                        .setLineItemId(lineItem.getId())
                        .setLineItem(lineItem)
                        .setPrice(lineItem.getDiscountedPrice().stripTrailingZeros())
                        .setOriginalPrice(lineItem.getPrice().stripTrailingZeros())
                        .setMaximumRefundableQuantity(lineItem.getQuantity() - refundedQuantity);
                refundableLineItems.add(refundLineItem);
            }
        }
        return refundableLineItems;
    }


    record RefundItemResult(
            List<RefundCalculateResponse.LineItem> refundableLineItems,
            List<RefundCalculateResponse.LineItem> refundLineItems
    ) {
        public static RefundItemResult EMPTY = new RefundItemResult(List.of(), List.of());

        public static RefundItemResult of(
                List<RefundCalculateResponse.LineItem> refundableLineItems,
                List<RefundCalculateResponse.LineItem> refundLineItems
        ) {
            return new RefundItemResult(refundableLineItems, refundLineItems);
        }
    }

}
