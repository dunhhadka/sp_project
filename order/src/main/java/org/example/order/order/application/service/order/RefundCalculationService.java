package org.example.order.order.application.service.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.order.request.RefundRequest;
import org.example.order.order.application.model.order.response.RefundCalculateResponse;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.model.TaxLine;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundCalculationService {

    private final ProductDao productDao;

    /**
     *
     */
    public RefundCalculateResponse calculateRefund(Order order, RefundRequest refundRequest) {
        var shipping = suggestRefundShipping(order, refundRequest.getShipping());

        var refundResult = suggestRefund(order, refundRequest);

        return null;
    }

    /**
     *
     */

    private <T> List<T> safeSelect(List<T> resource, Predicate<T> condition) {
        if (CollectionUtils.isEmpty(resource)) return List.of();
        return resource.stream().filter(condition).toList();
    }

    /**
     * khi refund line cần validate:
     * - restock type:
     * .    . + default các input nếu request không có
     * .    . +
     * - refund item
     * - location
     */
    private RefundResult suggestRefund(Order order, RefundRequest refundRequest) {
        var requestLines = safeSelect(refundRequest.getRefundLineItems(), line -> line.getQuantity() > 0);
        if (CollectionUtils.isEmpty(requestLines)) {
            return RefundResult.EMPTY;
        }

        var refundableItems = getRefundableItems(order);
        if (CollectionUtils.isEmpty(refundableItems)) {
            throw new ConstrainViolationException(UserError.builder()
                    .message("")
                    .build());
        }
        // set lại valid restock cho request_item
        forceRestockType(refundRequest.getRefundLineItems(), order.getId(), refundableItems);

        // validate item
        validateRefundItems(refundRequest.getRefundLineItems(), refundableItems);

        return new RefundResult(List.of(), refundableItems);
    }

    private void validateRefundItems(List<RefundRequest.LineItem> refundLineItems, List<RefundCalculateResponse.LineItem> refundableItems) {
        var refundLineItemMap = reduceRequestItems(refundLineItems);
        for (var entry : refundLineItemMap.entrySet()) {
            int lineItemId = entry.getKey();

            RefundCalculateResponse.LineItem lineItem = refundableItems.stream()
                    .filter(line -> Objects.equals(lineItemId, line.getLineItemId()))
                    .findFirst()
                    .orElse(null);
            if (lineItem == null) {
                throw new ConstrainViolationException(UserError.builder()
                        .code("not found")
                        .message("line item refund not found")
                        .fields(List.of("line_item_id"))
                        .build());
            }

            var model = entry.getValue();
            if (model.quantity > lineItem.getMaximumRefundableQuantity()) {
                throw new ConstrainViolationException(UserError.builder()
                        .message("")
                        .build());
            }
            if (model.removeQuantity > lineItem.getLineItem().getFulfillableQuantity()) {
                throw new ConstrainViolationException(UserError.builder()
                        .message("")
                        .build());
            }
        }
    }

    private Map<Integer, RefundItemValidationModel> reduceRequestItems(List<RefundRequest.LineItem> refundLineItems) {
        Map<Integer, RefundItemValidationModel> models = new HashMap<>();
        for (var item : refundLineItems) {
            int lineItemId = item.getLineItemId();

            var model = models.get(lineItemId);
            if (model == null) {
                model = new RefundItemValidationModel(item);
                models.put(lineItemId, model);
                continue;
            }
            model.add(item);
            models.put(lineItemId, model);
        }
        return models;
    }

    @Getter
    private static class RefundItemValidationModel {
        private int quantity;
        private int removeQuantity;

        public RefundItemValidationModel(RefundRequest.LineItem item) {
            add(item);
        }

        public void add(RefundRequest.LineItem item) {
            this.quantity += item.getQuantity();
            if (item.isRemoval()) {
                this.removeQuantity += item.getQuantity();
            }
        }
    }

    private void forceRestockType(
            List<RefundRequest.LineItem> refundItems,
            OrderId orderId,
            List<RefundCalculateResponse.LineItem> refundableItems
    ) {
        forceDefaultRestockType(refundItems);

        forceRestockTypeWithProduct(refundItems, orderId, refundableItems);
    }

    private void forceRestockTypeWithProduct(
            List<RefundRequest.LineItem> refundItems,
            OrderId orderId,
            List<RefundCalculateResponse.LineItem> refundableItems
    ) {
        var lineItemMap = refundableItems.stream()
                .map(RefundCalculateResponse.LineItem::getLineItem)
                .collect(Collectors.toMap(
                        LineItem::getId,
                        Function.identity()));

        Map<Integer, Integer> variantMap = new HashMap<>();
        for (var item : refundItems) {
            int lineItemId = item.getLineItemId();
            LineItem lineItem = lineItemMap.get(lineItemId);
            // validate not here
            if (lineItem == null)
                continue;

            Integer variantId = lineItem.getVariantInfo().getVariantId();
            if (variantId != null) {
                variantMap.put(lineItemId, variantId);
                continue;
            }

            item.setRestockType(RefundLineItem.RestockType.no_restock);
            item.setRemoval(true);
        }

        if (!variantMap.isEmpty()) {
            List<Integer> variantIds = variantMap.values().stream().distinct().toList();

            int storeId = orderId.getStoreId();
            List<VariantDto> variants = productDao.findVariantByListIds(storeId, variantIds);

            refundableItems.stream()
                    .filter(item -> variantMap.containsKey(item.getLineItemId()))
                    .forEach(item -> {
                        int variantId = variantMap.get(item.getLineItemId());
                        var variant = variants.stream()
                                .filter(v -> Objects.equals(v.getId(), variantId))
                                .findFirst()
                                .orElse(null);
                        if (variant == null) {
                            item.setRestockType(RefundLineItem.RestockType.no_restock);
                            item.setRemoval(true);
                        }
                    });
        }
    }

    private void forceDefaultRestockType(List<RefundRequest.LineItem> refundItems) {
        for (var item : refundItems) {
            if (item.getRestockType() == null) {
                item.setRestockType(RefundLineItem.RestockType.no_restock);
            }
            switch (item.getRestockType()) {
                case _return -> item.setRemoval(false);
                case cancel -> item.setRemoval(true);
            }
        }
    }

    private List<RefundCalculateResponse.LineItem> getRefundableItems(Order order) {
        List<RefundCalculateResponse.LineItem> refundableLineItems = new ArrayList<>();

        Map<Integer, Integer> refundedLineItems = new HashMap<>();
        if (CollectionUtils.isNotEmpty(order.getRefunds())) {
            order.getRefunds().stream()
                    .filter(refund -> CollectionUtils.isNotEmpty(refund.getRefundLineItems()))
                    .flatMap(refund -> refund.getRefundLineItems().stream())
                    .forEach(refundLine -> refundedLineItems.merge(refundLine.getLineItemId(), refundLine.getQuantity(), Integer::sum));
        }

        for (var lineItem : order.getLineItems()) {
            int lineItemId = lineItem.getId();
            int lineItemQuantity = lineItem.getQuantity();

            int refundedQuantity = refundedLineItems.getOrDefault(lineItemId, 0);
            int remainingQuantity = lineItemQuantity - refundedQuantity;
            if (remainingQuantity <= 0)
                continue;

            var refundItem = RefundCalculateResponse.LineItem.builder()
                    .lineItem(lineItem)
                    .lineItemId(lineItemId)
                    .quantity(lineItemQuantity)
                    .maximumRefundableQuantity(remainingQuantity)
                    .price(lineItem.getPrice())
                    .subtotal(lineItem.getSubtotalLinePrice())
                    .discountedPrice(lineItem.getDiscountedPrice())
                    .build();
            refundableLineItems.add(refundItem);
        }

        return refundableLineItems;
    }

    private record RefundResult(List<RefundCalculateResponse.LineItem> refundItems,
                                List<RefundCalculateResponse.LineItem> refundableItems) {
        public static final RefundResult EMPTY = new RefundResult(List.of(), List.of());
    }

    /**
     * Tính toán lại
     */
    private RefundCalculateResponse.Shipping suggestRefundShipping(Order order, RefundRequest.Shipping refundShippingRequest) {
        RefundCalculateResponse.Shipping suggestion = new RefundCalculateResponse.Shipping();

        BigDecimal totalShipping = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalShippingRefunded = BigDecimal.ZERO;
        BigDecimal totalTaxRefunded = BigDecimal.ZERO;

        if (CollectionUtils.isNotEmpty(order.getShippingLines())) {
            for (var shipping : order.getShippingLines()) {
                totalShipping = totalShipping.add(shipping.getPrice());

                if (CollectionUtils.isEmpty(shipping.getTaxLines()))
                    continue;
                totalTax = totalTax.add(shipping.getTaxLines().stream()
                        .map(TaxLine::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        }

        if (CollectionUtils.isNotEmpty(order.getRefunds())) {
            List<OrderAdjustment> orderAdjustments = order.getRefunds().stream()
                    .filter(refund -> CollectionUtils.isNotEmpty(refund.getOrderAdjustments()))
                    .flatMap(refund -> refund.getOrderAdjustments().stream())
                    .filter(oa -> oa.getRefundKind() == OrderAdjustment.RefundKind.shipping_refund)
                    .toList();
            for (var adjustment : orderAdjustments) {
                totalShippingRefunded = totalShippingRefunded.add(adjustment.getAmount());

                // full_amount khi hoàn tiền shipping = amount + tax_amount
                // nếu taxes_included => total_refund = amount + tax_amount
                if (order.isTaxIncluded()) {
                    totalShippingRefunded = totalShippingRefunded.add(adjustment.getTaxAmount());
                }

                totalTaxRefunded = totalTaxRefunded.add(adjustment.getTaxAmount());
            }
        }

        // calculate refund
        suggestion.setMaximumRefundable(totalShipping.subtract(totalShippingRefunded));
        if (refundShippingRequest != null) {
            int accuracyRounding = order.getMoneyInfo().getCurrency().getDefaultFractionDigits();

            // ưu tiên tính theo amount
            if (NumberUtils.isPositive(refundShippingRequest.getAmount())) {
                suggestion.setAmount(
                        refundShippingRequest.getAmount().setScale(accuracyRounding, RoundingMode.FLOOR)
                );
            } else if (BooleanUtils.isTrue(refundShippingRequest.getFullRefund())) {
                suggestion.setAmount(suggestion.getMaximumRefundable());
            }

            if (!NumberUtils.isPositive(suggestion.getAmount())) {
                throw new ConstrainViolationException(UserError.builder()
                        .message("require amount option for refund")
                        .fields(List.of("shipping"))
                        .build());
            }

            int compareAmountResult = suggestion.getAmount()
                    .compareTo(suggestion.getMaximumRefundable());
            if (compareAmountResult > 0) {
                throw new ConstrainViolationException(UserError.builder()
                        .message("amount must be less than or equal to maximum_refundable")
                        .fields(List.of("amount"))
                        .build());
            }

            BigDecimal refundTax;
            if (compareAmountResult == 0) {
                refundTax = totalTax.subtract(totalTaxRefunded);
            } else {
                refundTax = suggestion.getAmount().multiply(totalTax)
                        .divide(totalShipping, accuracyRounding, RoundingMode.FLOOR);
            }
            suggestion.setTax(refundTax.setScale(accuracyRounding, RoundingMode.FLOOR));
        }

        // strip zero
        suggestion
                .setAmount(suggestion.getAmount().stripTrailingZeros())
                .setTax(suggestion.getTax().stripTrailingZeros())
                .setMaximumRefundable(suggestion.getMaximumRefundable().stripTrailingZeros());

        return suggestion;
    }
}