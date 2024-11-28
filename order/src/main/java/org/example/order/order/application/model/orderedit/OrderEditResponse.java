package org.example.order.order.application.model.orderedit;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.model.order.response.OrderResponse;

import java.util.List;

@Getter
@Setter
@Builder
public class OrderEditResponse {
    // end state previews after an mutation
    private CalculatedOrder calculatedOrder;
    // result of add_variant, add_custom_item, increment_item, decrement_item
    private List<CalculatedLineItem> calculatedLineItems;
    // result of add_item_discount
    private OrderStagedChangeModel stagedChange;
    private List<OrderStagedChangeModel> stagedChanges;
    // committed order
    private OrderResponse order;
}
