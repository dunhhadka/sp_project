package org.example.order.order.application.service.fulfillment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.fulfillment.model.FulfillmentRepository;
import org.example.order.order.domain.refund.event.RefundCreatedAppEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FulfillmentSideEffectService {

    private final FulfillmentRepository fulfillmentRepository;

    // Xử lý cho case tại order-return
    @Transactional
    @EventListener(RefundCreatedAppEvent.class)
    public void handleOrderRefundCreated(RefundCreatedAppEvent event) {
        if (NumberUtils.isPositive(event.getReturnId())) {
            return;
        }
        var storeId = event.getStoreId();
        var orderId = (int) event.getOrderId();
        var restockLineItems = event.getRestockLineItems();
        // tính rstock_line quantity nếu line đó đã fulfilled
        var effectiveQuantityMap = restockLineItems.stream()
                .filter(RefundCreatedAppEvent.RestockLineItem::isRemoval)
                .collect(Collectors.groupingBy(RefundCreatedAppEvent.RestockLineItem::lineItemId,
                        Collectors.summingInt(RefundCreatedAppEvent.RestockLineItem::quantity)));
        if (CollectionUtils.isEmpty(effectiveQuantityMap)) return;

        var fulfillments = fulfillmentRepository.getByOrderId(storeId, orderId);
        var successFulfillments = fulfillments.stream()
                .filter(ff -> Fulfillment.FulfillStatus.success == ff.getFulfillStatus())
                .toList();
        if (CollectionUtils.isEmpty(successFulfillments)) {
            return;
        }

        for (var fulfillment : fulfillments) {
            Map<Long, Integer> modifiedMap = new HashMap<>();
            var fulfillmentLineItems = fulfillment.getLineItems();
            for (var fulfillmentLine : fulfillmentLineItems) {
                var fulfillmentLineItemId = (long) fulfillmentLine.getLineItemId();
                var effectiveQuantity = effectiveQuantityMap.get(fulfillmentLineItemId);
                if (!NumberUtils.isPositive(effectiveQuantity)) {
                    continue;
                }
                var modifiedQuantity = Math.min(fulfillmentLine.getEffectiveQuantity(), effectiveQuantity);
                if (modifiedQuantity > 0) {
                    modifiedMap.put(fulfillmentLineItemId, modifiedQuantity);
                    effectiveQuantityMap.put((int) fulfillmentLineItemId, effectiveQuantity - modifiedQuantity);
                }
            }
            fulfillment.updateEffectiveQuantity(modifiedMap, Fulfillment.EffectQuantityType.subtract);
            fulfillmentRepository.save(fulfillment);
        }
    }
}
