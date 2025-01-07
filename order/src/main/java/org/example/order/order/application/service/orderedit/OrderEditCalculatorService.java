package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.CalculatedOrder;
import org.example.order.order.domain.order.model.DiscountAllocation;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.domain.order.model.TaxLine;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dao.*;
import org.example.order.order.infrastructure.data.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderEditCalculatorService {

    private final OrderDao orderDao;
    private final LineItemDao lineItemDao;
    private final DiscountAllocationDao discountAllocationDao;
    private final DiscountApplicationDao discountApplicationDao;
    private final TaxLineDao taxLineDao;
    private final RefundTaxLineDao refundTaxLineDao;

    private final OrderEditDao orderEditDao;
    private final OrderEditLineItemDao editLineItemDao;
    private final OrderEditDiscountAllocationDao editDiscountAllocationDao;
    private final OrderEditDiscountApplicationDao editDiscountApplicationDao;
    private final OrderEditTaxLineDao editTaxLineDao;
    private final OrderStagedChangeDao stagedChangeDao;

    /**
     * B1: get tất cả dữ liệu cần sử dụng ở sau
     * B2. Tạo context:
     * - group change, phân loại các change sau đó group thành 1, sử dụng thì get ra
     * - group taxLine, nếu 2 taxLine có 2 key giống nhau thì gộp thành 1 = tổng amount của 2 taxLine === key(title, rate, custom)
     * B3:
     * - build addedLineItems: 1 lineItem có thể gồm 2 action => addVariant or addItemCustom và addItemDiscount => tạo context là mapAddVariant, mapAddItemDiscount
     * - build orderLineItems: 1 lineItem có thể gồm 1 trong 2 action (incrementItem or decrementItem) => tạo context là mapQuantityAdjustment
     * -    để build được addLineItem cần: orderEditAddedLineItem, action addItems, action addItemDiscount, newTaxLines, discountAllocations nếu có
     * -    để build được orderLineItem cần : orderLineItem, action quantity, discountAllocation, currentTax, newTaxLines nếu có
     * -    đầu ra là calculatedLineItem
     * -    khi tạo builder thì cần có 2 method là 2 step là builder là build
     * -
     */
    public CalculatedOrder calculateResponse(OrderEditId editId) {
        EntityGraph entityGraph = fetchEntityGraph(editId);

        EditContext context = buildContext(entityGraph);

        CalculatedOrder calculatedOrder = mapToOrderEdit(entityGraph.orderEdit.orderEdit);

        List<BuilderSteps.BuildResult> addedResults = entityGraph.orderEdit.lineItems.stream()
                .map(line -> buildAddedLine(line, context))
                .toList();

        return calculatedOrder;
    }

    private BuilderSteps.BuildResult buildAddedLine(OrderEditLineItemDto line, EditContext context) {
        var addedContext = buildAddedLineContext(line.getId(), context);
        return AddedLineItemBuilder
                .forLineItem(
                        line,
                        addedContext)
                .build();
    }

    private AddedLineItemBuilder.Context buildAddedLineContext(UUID lineItemId, EditContext context) {
        String lineItemIdString = lineItemId.toString();

        List<OrderEditTaxLineDto> addedTaxLines = context.addedTaxLineMap.get(lineItemIdString);
        List<OrderEditDiscountAllocationDto> discountAllocations = context.discountMap.get(lineItemId);

        OrderStagedChange.AddLineItemAction action = context.addLineItemActionMap.get(lineItemId);
        List<OrderStagedChange.AddItemDiscount> addDiscount = context.stagedChange.addItemDiscounts();

        AddedLineItemBuilder.Changes changes = new AddedLineItemBuilder.Changes(action, addDiscount);

        return new AddedLineItemBuilder.Context(
                addedTaxLines,
                discountAllocations,
                changes
        );
    }

    private CalculatedOrder mapToOrderEdit(OrderEditDto resource) {
        var target = new CalculatedOrder();
        target.setId(resource.getId());
        target.setStoreId(resource.getStoreId());
        target.setOrderId(resource.getOrderId());
        target.setCommitted(resource.isCommitted());
        target.setSubtotalLineItemQuantity(resource.getSubtotalLineItemsQuantity());
        target.setSubtotalPrice(resource.getSubtotalPrice());
        target.setCartDiscountAmount(resource.getCartDiscountAmount());
        target.setTotalPrice(resource.getTotalPrice());
        target.setTotalOutStanding(resource.getTotalOutstanding());
        return target;
    }


    private EditContext buildContext(EntityGraph entityGraph) {
        List<OrderStagedChangeDto> changes = entityGraph.orderEdit.changes;
        OrderEditUtils.GroupedStagedChange stagedChange = OrderEditUtils.groupStagedChange(changes);

        // for order
        var quantityAdjustments = stagedChange.quantityAdjustmentStream()
                .collect(Collectors.toMap(
                        OrderStagedChange.QuantityAdjustmentAction::getLineItemId,
                        Function.identity()));

        var allocations = entityGraph.order.discountAllocations.stream()
                .filter(discount -> discount.getTargetType() == DiscountAllocation.TargetType.line_item)
                .collect(Collectors.groupingBy(DiscountAllocationDto::getTargetId));

        BigDecimal orderAndShippingDiscount = entityGraph.order.discountAllocations.stream()
                .filter(isShippingDiscount().or(isOrderDiscount(entityGraph.order.discountApplications)))
                .map(DiscountAllocationDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var existingTaxLines = entityGraph.order.taxLines.stream()
                .filter(taxLine -> taxLine.getTargetType() == TaxLine.TargetType.line_item)
                .collect(Collectors.groupingBy(
                        TaxLineDto::getTargetId,
                        Collectors.mapping(taxLine -> taxLine, Collectors.toList())));

        var refundTaxMap = entityGraph.order.refundTaxLines.stream()
                .collect(Collectors.groupingBy(RefundTaxLineDto::getTaxLineId));

        var shippingTaxes = entityGraph.order.taxLines.stream()
                .filter(taxLine -> taxLine.getTargetType() == TaxLine.TargetType.shipping_line)
                .collect(MergedTaxLine.toMergeMap());

        // for order-edit
        var discounts = stagedChange.addItemDiscounts().stream()
                .collect(Collectors.groupingBy(OrderStagedChange.AddItemDiscount::getLineItemId));

        var added = stagedChange.addActionsStream()
                .collect(Collectors.toMap(
                        OrderStagedChange.AddLineItemAction::getLineItemId,
                        Function.identity()));

        var addedTaxLineMap = entityGraph.orderEdit.taxLines.stream()
                .collect(Collectors.groupingBy(OrderEditTaxLineDto::getLineItemId));

        var addedDiscountMap = entityGraph.orderEdit.discountAllocations.stream()
                .collect(Collectors.groupingBy(OrderEditDiscountAllocationDto::getLineItemId));

        return new EditContext(
                stagedChange,
                quantityAdjustments,
                allocations,
                orderAndShippingDiscount,
                existingTaxLines,
                refundTaxMap,
                shippingTaxes,
                discounts,
                added,
                addedTaxLineMap,
                addedDiscountMap
        );
    }

    private Predicate<DiscountAllocationDto> isOrderDiscount(List<DiscountApplicationDto> applications) {
        return allocation -> {
            var application = applications.get(allocation.getApplicationIndex());
            return application.getRuleType() == DiscountApplication.RuleType.order;
        };
    }

    private Predicate<DiscountAllocationDto> isShippingDiscount() {
        return allocation -> allocation.getTargetType() == DiscountAllocation.TargetType.shipping_line;
    }

    private EntityGraph fetchEntityGraph(OrderEditId editId) {
        EditGraph editGraph = getEditGraph(editId);

        int orderId = editGraph.orderEdit.getOrderId();
        OrderGraph orderGraph = getOrderGraph(editId.getStoreId(), orderId);

        return new EntityGraph(orderGraph, editGraph);
    }

    private OrderGraph getOrderGraph(int storeId, int orderId) {
        return new OrderGraph(
                orderDao.findById(storeId, orderId),
                lineItemDao.getByOrderIds(storeId, List.of(orderId)),
                discountApplicationDao.getByOrderIds(storeId, List.of(orderId)),
                discountAllocationDao.getByOrderIds(storeId, List.of(orderId)),
                taxLineDao.getByOrderIds(storeId, List.of(orderId)),
                refundTaxLineDao.getByStoreIdAndOrderIds(storeId, List.of(orderId))
        );
    }

    private EditGraph getEditGraph(OrderEditId editId) {
        int storeId = editId.getStoreId();
        int editingId = editId.getId();
        return new EditGraph(
                orderEditDao.getById(storeId, editingId),
                editLineItemDao.getByEditingId(storeId, editingId),
                editDiscountApplicationDao.getByEditingId(storeId, editingId),
                editDiscountAllocationDao.getByEditingId(storeId, editingId),
                editTaxLineDao.getByEditingId(storeId, editingId),
                stagedChangeDao.getByEditingId(storeId, editingId)
        );
    }

    // region record

    private record EditContext(
            OrderEditUtils.GroupedStagedChange stagedChange,
            Map<Integer, OrderStagedChange.QuantityAdjustmentAction> quantityAdjustments,
            Map<Integer, List<DiscountAllocationDto>> allocations,
            BigDecimal orderAndShippingDiscount,
            Map<Integer, List<TaxLineDto>> existingTaxLines,
            Map<Integer, List<RefundTaxLineDto>> refundTaxMap,
            Map<MergedTaxLine.TaxLineKey, MergedTaxLine> shippingTaxes,
            Map<UUID, List<OrderStagedChange.AddItemDiscount>> addedDiscountMap,
            Map<UUID, OrderStagedChange.AddLineItemAction> addLineItemActionMap,
            Map<String, List<OrderEditTaxLineDto>> addedTaxLineMap,
            Map<UUID, List<OrderEditDiscountAllocationDto>> discountMap) {
    }

    private record EntityGraph(
            OrderGraph order,
            EditGraph orderEdit
    ) {
    }

    private record OrderGraph(
            OrderDto order,
            List<LineItemDto> lineItems,
            List<DiscountApplicationDto> discountApplications,
            List<DiscountAllocationDto> discountAllocations,
            List<TaxLineDto> taxLines,
            List<RefundTaxLineDto> refundTaxLines
    ) {
    }

    private record EditGraph(
            OrderEditDto orderEdit,
            List<OrderEditLineItemDto> lineItems,
            List<OrderEditDiscountApplicationDto> discountApplications,
            List<OrderEditDiscountAllocationDto> discountAllocations,
            List<OrderEditTaxLineDto> taxLines,
            List<OrderStagedChangeDto> changes
    ) {
    }

    // end region record
}
