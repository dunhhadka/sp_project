package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.CalculatedOrder;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.infrastructure.data.dao.*;
import org.example.order.order.infrastructure.data.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

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

        EditContext context = buildEditContext(entityGraph);

        CalculatedOrder calculatedOrder = mapToOrderEdit(entityGraph.orderEdit.orderEdit);

        return calculatedOrder;
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


    private EditContext buildEditContext(EntityGraph entityGraph) {
        List<OrderStagedChangeDto> changes = entityGraph.orderEdit.changes;
        OrderEditUtils.GroupedStagedChange stagedChange = OrderEditUtils.groupStagedChange(changes);

        return new EditContext(
                stagedChange
        );
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
            OrderEditUtils.GroupedStagedChange stagedChange
    ) {
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
