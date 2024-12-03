package org.example.order.order.application.service.orderedit;

import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.*;
import org.example.order.order.domain.order.model.DiscountAllocation;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.domain.order.model.TaxLine;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dao.*;
import org.example.order.order.infrastructure.data.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrderEditCalculatorService {

    private final OrderDao orderDao;
    private final LineItemDao lineItemDao;
    private final DiscountApplicationDao orderDiscountApplicationDao;
    private final DiscountAllocationDao orderDiscountAllocationDao;
    private final TaxLineDao taxLineDao;
    private final RefundTaxLineDao refundTaxLineDao;

    private final OrderEditDao orderEditDao;
    private final OrderEditLineItemDao editLineItemDao;
    private final OrderEditDiscountAllocationDao editDiscountAllocationDao;
    private final OrderEditDiscountApplicationDao editDiscountApplicationDao;
    private final OrderEditTaxLineDao editTaxLineDao;
    private final OrderStagedChangeDao orderStagedChangeDao;

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
    public CalculatedOrder calculate(OrderEditId orderEditId) {
        EntityGraph entityGraph = fetchEntityGraph(orderEditId);

        EditContext context = buildEditContext(entityGraph);

        CalculatedOrder calculatedOrder = mapFromOrderEdit(entityGraph.orderEdit.orderEdit);

        List<BuilderSteps.BuilderResult> addedResults = entityGraph.orderEdit.editLineItems.stream()
                .map(line -> buildAddedLineItem(line, context))
                .toList();
        List<CalculatedLineItem> addedLineItems = addedResults.stream().map(BuilderSteps.BuilderResult::lineItem).toList();
        calculatedOrder.setAddedLineItems(addedLineItems);

        List<BuilderSteps.BuilderResult> existedLineResults = entityGraph.order.lineItems.stream()
                .map(line -> buildExistedLine(line, context))
                .toList();
        var lineItems = existedLineResults.stream().map(BuilderSteps.BuilderResult::lineItem).toList();
        var fulfilledLineItems = lineItems.stream().filter(line -> line.getEditableQuantity() <= 0).toList();
        var unFulfilledLineItems = lineItems.stream().filter(line -> line.getEditableQuantity() > 0).toList();
        calculatedOrder.setLineItems(lineItems);
        calculatedOrder.setUnfulfilledLineItems(unFulfilledLineItems);
        calculatedOrder.setFulfilledLineItems(fulfilledLineItems);

        List<OrderStagedChangeModel> changes = entityGraph.orderEdit.changes.stream()
                .map(StagedChangeModelMapper::toModel)
                .toList();
        calculatedOrder.setStagedChanges(changes);

        List<CalculatedDiscountApplication> discountApplications = entityGraph.orderEdit.editDiscountApplications.stream()
                .map(CalculatedDiscountApplication::new)
                .toList();
        calculatedOrder.setAddedDiscountApplication(discountApplications);

        var taxLines = Streams
                .concat(
                        addedResults.stream().map(BuilderSteps.BuilderResult::taxLines),
                        existedLineResults.stream().map(BuilderSteps.BuilderResult::taxLines),
                        Stream.of(context.shippingTaxLines))
                .collect(MergedTaxLine.mergeMaps());

        return calculatedOrder;
    }

    private BuilderSteps.BuilderResult buildExistedLine(LineItemDto orderLine, EditContext context) {
        return LineItemBuilder
                .forLineItem(
                        orderLine,
                        buildLineItemContext(orderLine, context))
                .build();
    }

    private LineItemBuilder.Context buildLineItemContext(LineItemDto orderLine, EditContext context) {
        Integer lineItemId = orderLine.getId();

        var action = context.quantityAdjustmentActionMap.get(lineItemId);

        var allocations = context.existedDiscountMap.get(lineItemId);

        var existingTaxLines = context.existingTaxLines.get(lineItemId);

        var newTaxes = context.newTaxes.get(String.valueOf(lineItemId));

        return new LineItemBuilder.Context(
                action,
                allocations,
                existingTaxLines,
                newTaxes,
                orderLine,
                context.isProductDiscount
        );
    }

    private BuilderSteps.BuilderResult buildAddedLineItem(OrderEditLineItemDto addedLineItem, EditContext context) {
        UUID lineItemId = addedLineItem.getId();
        OrderStagedChange.AddLineItemAction addAction = context.addLineItemMap.get(lineItemId);
        OrderStagedChange.AddItemDiscount addItemDiscount = context.addItemDiscountMap.get(lineItemId);

        AddedLineItemBuilder.Changes changes = new AddedLineItemBuilder.Changes(addAction, addItemDiscount);

        return AddedLineItemBuilder
                .forLineItem(
                        addedLineItem,
                        new AddedLineItemBuilder.Context(
                                context.newTaxes.get(lineItemId.toString()),
                                context.addedDiscountMap.get(lineItemId), changes))
                .build();
    }

    private CalculatedOrder mapFromOrderEdit(OrderEditDto resource) {
        CalculatedOrder target = new CalculatedOrder();
        target.setId(resource.getId());
        target.setStoreId(resource.getStoreId());
        target.setOrderId(resource.getOrderId());
        target.setSubtotalLineItemQuantity(resource.getSubtotalLineItemsQuantity());
        target.setSubtotalPrice(resource.getSubtotalPrice());
        target.setCartDiscountAmount(resource.getCartDiscountAmount());
        target.setTotalPrice(resource.getTotalPrice());
        target.setTotalOutStanding(resource.getTotalOutstanding());
        return target;
    }

    private EditContext buildEditContext(EntityGraph entityGraph) {
        List<OrderStagedChangeDto> stagedChanges = entityGraph.orderEdit.changes();
        var changes = OrderEditUtils.groupChanges(stagedChanges.stream().map(OrderEditUtils::convert).toList());

        var addLineItemMap = changes.getAddItemActions()
                .collect(Collectors.toMap(
                        OrderStagedChange.AddLineItemAction::getLineItemId,
                        Function.identity()));

        var addItemDiscountMap = changes.addItemDiscounts().stream()
                .collect(Collectors.toMap(
                        OrderStagedChange.AddItemDiscount::getLineItemId,
                        Function.identity()));

        var quantityAdjustmentMap = changes.getAdjustQuantityChanges()
                .collect(Collectors.toMap(
                        OrderStagedChange.QuantityAdjustmentAction::getLineItemId,
                        Function.identity()));

        var addedDiscountMap = entityGraph.orderEdit.editDiscountAllocations.stream()
                .collect(Collectors.groupingBy(OrderEditDiscountAllocationDto::getLineItemId));

        var newTaxes = entityGraph.orderEdit.editTaxLines.stream()
                .collect(Collectors.toMap(
                        OrderEditTaxLineDto::getLineItemId,
                        Function.identity()));

        var shippingTaxLines = entityGraph.order.taxLines.stream()
                .filter(tax -> tax.getTargetType() == TaxLine.TargetType.shipping_line)
                .collect(MergedTaxLine.toMergedMap());

        var refundedTaxLines = entityGraph.order.refundTaxLines.stream()
                .collect(Collectors.groupingBy(RefundTaxLineDto::getTaxLineId));
        var existingTaxLines = entityGraph.order.taxLines.stream()
                .filter(tax -> tax.getTargetType() == TaxLine.TargetType.line_item)
                .collect(Collectors.groupingBy(
                        TaxLineDto::getTargetId,
                        Collectors.mapping(
                                tax -> new LineItemBuilder.TaxLineInfo(tax, refundedTaxLines.get(tax.getId())),
                                Collectors.toList()
                        )));

        var existedDiscountMap = entityGraph.order.allocations.stream()
                .filter(discount -> discount.getTargetType() == DiscountAllocation.TargetType.line_item)
                .collect(Collectors.groupingBy(DiscountAllocationDto::getTargetId));

        var isProductDiscount = isProductDiscount(entityGraph.order.applications);

        return new EditContext(
                changes,
                addLineItemMap,
                addItemDiscountMap,
                quantityAdjustmentMap,
                addedDiscountMap,
                newTaxes,
                shippingTaxLines,
                existingTaxLines,
                refundedTaxLines,
                existedDiscountMap,
                isProductDiscount
        );
    }

    Predicate<DiscountAllocationDto> isProductDiscount(List<DiscountApplicationDto> applications) {
        return (allocation) -> {
            var application = applications.get(allocation.getApplicationIndex());
            if (allocation.getId() != allocation.getApplicationId()) {
                throw new IllegalArgumentException("Unexpected error discount");
            }
            return application.getRuleType() == DiscountApplication.RuleType.product;
        };
    }

    private EntityGraph fetchEntityGraph(OrderEditId orderEditId) {
        var storeId = orderEditId.getStoreId();
        var editId = orderEditId.getId();
        OrderEditGraph orderEdit = getOrderEditGraph(storeId, editId);

        var orderId = orderEdit.orderEdit.getOrderId();
        OrderGraph order = getOrderGraph(storeId, orderId);

        return new EntityGraph(order, orderEdit);
    }

    private OrderGraph getOrderGraph(int storeId, int orderId) {
        return new OrderGraph(
                orderDao.findById(storeId, orderId),
                lineItemDao.getByOrderIds(storeId, List.of(orderId)),
                orderDiscountApplicationDao.getByOrderIds(storeId, List.of(orderId)),
                orderDiscountAllocationDao.getByOrderIds(storeId, List.of(orderId)),
                taxLineDao.getByOrderIds(storeId, List.of(orderId)),
                refundTaxLineDao.getByStoreIdAndOrderIds(storeId, List.of(orderId))
        );
    }

    private OrderEditGraph getOrderEditGraph(int storeId, int editId) {
        return new OrderEditGraph(
                orderEditDao.getById(storeId, editId),
                editLineItemDao.getByEditingId(storeId, editId),
                editDiscountApplicationDao.getByEditingId(storeId, editId),
                editDiscountAllocationDao.getByEditingId(storeId, editId),
                editTaxLineDao.getByEditingId(storeId, editId),
                orderStagedChangeDao.getByEditingId(storeId, editId)
        );
    }

    // region record

    record EditContext(
            OrderEditUtils.GroupedStagedChange groupedChange,
            Map<UUID, OrderStagedChange.AddLineItemAction> addLineItemMap,
            Map<UUID, OrderStagedChange.AddItemDiscount> addItemDiscountMap,
            Map<Integer, OrderStagedChange.QuantityAdjustmentAction> quantityAdjustmentActionMap,
            Map<UUID, List<OrderEditDiscountAllocationDto>> addedDiscountMap, // support cho 1 discount,
            Map<String, OrderEditTaxLineDto> newTaxes,
            Map<MergedTaxLine.TaxLineKey, MergedTaxLine> shippingTaxLines,
            Map<Integer, List<LineItemBuilder.TaxLineInfo>> existingTaxLines,
            Map<Integer, List<RefundTaxLineDto>> refundedTaxLines,
            Map<Integer, List<DiscountAllocationDto>> existedDiscountMap,
            Predicate<DiscountAllocationDto> isProductDiscount) {
    }

    record EntityGraph(OrderGraph order, OrderEditGraph orderEdit) {
    }

    record OrderGraph(
            OrderDto order,
            List<LineItemDto> lineItems,
            List<DiscountApplicationDto> applications,
            List<DiscountAllocationDto> allocations,
            List<TaxLineDto> taxLines,
            List<RefundTaxLineDto> refundTaxLines
    ) {
    }

    record OrderEditGraph(
            OrderEditDto orderEdit,
            List<OrderEditLineItemDto> editLineItems,
            List<OrderEditDiscountApplicationDto> editDiscountApplications,
            List<OrderEditDiscountAllocationDto> editDiscountAllocations,
            List<OrderEditTaxLineDto> editTaxLines,
            List<OrderStagedChangeDto> changes
    ) {

    }

    // end region record

}
