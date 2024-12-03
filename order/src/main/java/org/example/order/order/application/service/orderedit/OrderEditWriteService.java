package org.example.order.order.application.service.orderedit;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.orderedit.model.AddedLineItem;
import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.domain.orderedit.persistence.OrderEditRepository;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.infrastructure.data.dto.Location;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEditWriteService {

    private final OrderRepository orderRepository;
    private final OrderEditRepository orderEditRepository;
    private final OrderEditContextService orderEditContextService;
    private final OrderCommitService orderCommitService;

    /**
     * cartDiscountAmount = Giảm giá trong shipping và giảm giá đơn hàng trong line_item
     */
    @Transactional
    public OrderEditId beginEdit(OrderId orderId) {
        var order = getOrderById(orderId);
        var currency = order.getMoneyInfo().getCurrency();

        BigDecimal subtotalLineItemQuantity = BigDecimal.ZERO;

        BigDecimal cartDiscountAmount = BigDecimal.ZERO;
        BigDecimal productDiscountAmount = BigDecimal.ZERO;

        BigDecimal subtotalPrice = BigDecimal.ZERO;

        BigDecimal totalShippingPrice = BigDecimal.ZERO;
        BigDecimal shippingRefundAmount = BigDecimal.ZERO;

        BigDecimal totalTax = BigDecimal.ZERO;

        Map<Integer, Integer> refundedLineItem = new HashMap<>();
        for (var refund : order.getRefunds()) {
            for (var refundLine : refund.getRefundLineItems()) {
                refundedLineItem.merge(refundLine.getLineItemId(), refundLine.getQuantity(), Integer::sum);
            }
            if (CollectionUtils.isNotEmpty(refund.getOrderAdjustments())) {
                for (var adjustment : refund.getOrderAdjustments()) {
                    if (adjustment.getRefundKind() == OrderAdjustment.RefundKind.shipping_refund) {
                        var refundAmount = adjustment.getAmount().add(adjustment.getTaxAmount());
                        shippingRefundAmount = shippingRefundAmount.add(refundAmount);
                    }
                }
            }
        }

        for (var shipping : order.getShippingLines()) {
            totalShippingPrice = totalShippingPrice.add(shipping.getPrice());
            if (CollectionUtils.isNotEmpty(shipping.getDiscountAllocations())) {
                var shippingDiscountAmount = shipping.getDiscountAllocations().stream()
                        .map(DiscountAllocation::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                cartDiscountAmount = cartDiscountAmount.add(shippingDiscountAmount);
            }
        }

        for (var lineItem : order.getLineItems()) {
            var lineItemId = lineItem.getId();

            var originalQuantityDecimal = BigDecimal.valueOf(lineItem.getQuantity());
            var refundedQuantity = refundedLineItem.getOrDefault(lineItemId, 0);
            var refundedQuantityDecimal = BigDecimal.valueOf(refundedQuantity);
            var currentQuantity = originalQuantityDecimal.subtract(refundedQuantityDecimal);

            if (currentQuantity.signum() <= 0) continue;
            subtotalLineItemQuantity = subtotalLineItemQuantity.add(currentQuantity);

            var currentLinePrice = lineItem.getPrice().multiply(currentQuantity);
            subtotalPrice = subtotalPrice.add(currentLinePrice);

            BigDecimal productDiscount = BigDecimal.ZERO;
            BigDecimal cartDiscount = BigDecimal.ZERO;
            for (var discount : lineItem.getDiscountAllocations()) {
                if (filterCartDiscount(discount, order)) {
                    cartDiscount = cartDiscount.add(discount.getAmount());
                } else {
                    productDiscount = productDiscount.add(discount.getAmount());
                }
            }

            var effectiveProductDiscount = productDiscount
                    .multiply(currentQuantity)
                    .divide(originalQuantityDecimal, currency.getDefaultFractionDigits(), RoundingMode.CEILING);
            productDiscountAmount = productDiscountAmount.add(effectiveProductDiscount);

            var effectiveCartDiscount = cartDiscount
                    .multiply(currentQuantity)
                    .divide(originalQuantityDecimal, currency.getDefaultFractionDigits(), RoundingMode.CEILING);
            cartDiscountAmount = cartDiscountAmount.add(effectiveCartDiscount);

            var totalTaxLine = lineItem.getTaxLines().stream()
                    .map(TaxLine::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var effectiveTaxLinePrice = totalTaxLine
                    .multiply(currentQuantity)
                    .divide(originalQuantityDecimal, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            totalTax = totalTax.add(effectiveTaxLinePrice);
        }

        var totalPrice = subtotalPrice
                .add(totalShippingPrice)
                .add(totalTax)
                .subtract(shippingRefundAmount)
                .subtract(cartDiscountAmount)
                .subtract(productDiscountAmount);
        var totalOutStanding = order.getMoneyInfo().getTotalOutstanding();

        var orderEdit = new OrderEdit(
                order.getId(),
                currency,
                subtotalLineItemQuantity,
                subtotalPrice,
                cartDiscountAmount,
                totalPrice,
                totalOutStanding
        );

        orderEditRepository.save(orderEdit);

        return orderEdit.getId();
    }

    private boolean filterCartDiscount(DiscountAllocation discount, Order order) {
        var application = order.getDiscountApplications().get(discount.getApplicationIndex());
        Assert.isTrue(application.getId() == discount.getApplicationId(),
                "discount_application's order is incorrect");
        return application.getRuleType() == DiscountApplication.RuleType.order;
    }

    private Order getOrderById(OrderId orderId) {
        var order = orderRepository.findById(orderId);
        if (order == null) throw new ConstrainViolationException("order", "not found by id");
        if (order.getCancelledOn() != null) {
            throw new ConstrainViolationException("order", "cancelled_order can't be edit");
        }
        if (order.getClosedOn() != null) {
            throw new ConstrainViolationException("order", "closed_order can't be edit");
        }
        return order;
    }


    /**
     * addVariant :  {variantId, quantity, locationId, allowDuplicate} => context: productInfo, locationInfo, taxLineInfo
     */
    @Transactional
    public List<UUID> addVariants(OrderEditId editingId, OrderEditRequest.AddVariants addVariants) {
        var context = orderEditContextService.getAddVariantsContext(editingId, addVariants.getAddVariants());
        List<UUID> lineItemIds = addVariants(addVariants, context);
        orderEditRepository.save(context.orderEdit());
        return lineItemIds;
    }

    private List<UUID> addVariants(OrderEditRequest.AddVariants addVariants, OrderEditContextService.AddVariantsContext context) {
        return addVariants.getAddVariants().stream()
                .map(addVariant -> this.addVariant(addVariant, context))
                .toList();
    }

    private UUID addVariant(OrderEditRequest.AddVariant addVariant, OrderEditContextService.AddVariantsContext context) {
        var variant = context.getVariant(addVariant.getVariantId());
        var product = context.getProduct(variant.getProductId());

        var location = context.getEffectiveLocation(addVariant.getLocationId());

        var lineItem = buildLineItem(variant, product, location, addVariant);

        context.orderEdit().addLineItem(lineItem, context.taxSetting());

        return lineItem.getId();
    }

    private AddedLineItem buildLineItem(VariantDto variant, ProductDto product, Location location, OrderEditRequest.AddVariant addVariant) {
        return new AddedLineItem(
                addVariant.getQuantity(),
                variant.getId(),
                product.getId(),
                (int) location.getId(),
                variant.getSku(),
                product.getName(),
                variant.getTitle(),
                variant.getPrice(),
                variant.isTaxable(),
                variant.isRequiresShipping(),
                true
        );
    }

    /**
     * edit quantity có thể có edit 2 loại: edit addedLineItem, edit lineItem
     * => context: lineItem or addedLineItem, taxSetting, location
     */
    @Transactional
    public String increaseLineItem(OrderEditId orderEditId, OrderEditRequest.Increment increment) {
        var context = orderEditContextService.getIncrementLineItemContext(orderEditId, increment);

        var orderEdit = context.orderEdit();

        orderEdit.increaseLineItemQuantity(context, increment);

        orderEditRepository.save(orderEdit);
        return increment.getLineItemId();
    }

    @Transactional
    public String setItemQuantity(OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        var context = orderEditContextService.createSetItemQuantityContext(orderEditId, request);
        OrderEdit editing = context.orderEdit();

        if (context instanceof OrderEditContextService.AddedLineItemRemover remover) {
            editing.removeLineItem(remover.lineItem().getId(), remover.taxSetting());
        } else if (context instanceof OrderEditContextService.ChangeAddedLineItemQuantity changeQuantity) {
            var adjustmentLineItemId = changeQuantity.lineItem().getId();

            editing.adjustAddedLineQuantity(adjustmentLineItemId, request, changeQuantity.taxSetting());
        } else if (context instanceof OrderEditContextService.ExistingLineItem existingLineItem) {
            LineItem lineItem = existingLineItem.lineItem();

            // cannot adjust quantity => TH1: LineItem đã fulfilled , TH2: LineItem có Discount
            boolean isFulfilled = lineItem.getFulfillableQuantity() == 0;
            if (isFulfilled) {
                throw new ConstrainViolationException("line_item", "line item");
            }

            boolean hasDiscount = CollectionUtils.isNotEmpty(lineItem.getDiscountAllocations());
            if (hasDiscount) {
                throw new ConstrainViolationException(
                        "line_item",
                        "Line item has discount cannot edit");
            }

            editing.recordQuantityAdjustment(lineItem, existingLineItem.taxSetting(), request);

        }

        orderEditRepository.save(editing);
        return StringUtils.EMPTY;
    }

    @Transactional
    public void commit(OrderEditId orderEditId) {
        var orderEdit = orderEditRepository.findById(orderEditId);
        var order = orderRepository.findById(new OrderId(orderEditId.getStoreId(), orderEdit.getOrderId()));

        orderCommitService.commit(order, orderEdit);
    }
}
