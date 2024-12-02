package org.example.order.order.application.service.orderedit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LineItemUtils {

    public static List<OrderStagedChange> getChanges(List<OrderStagedChange> stagedChanges, int lineItemId) {
        List<OrderStagedChange> changes = new ArrayList<>();
        for (var change : stagedChanges) {
            OrderStagedChange.BaseAction action = change.getAction();
            if (action instanceof OrderStagedChange.QuantityAdjustmentAction adjustmentAction) {
                if (adjustmentAction.getLineItemId() == lineItemId) {
                    changes.add(change);
                    break;
                }
            }
        }
        return changes;
    }
}
