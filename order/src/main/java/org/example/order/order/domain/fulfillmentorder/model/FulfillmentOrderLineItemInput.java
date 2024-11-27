package org.example.order.order.domain.fulfillmentorder.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FulfillmentOrderLineItemInput {
    private int id;
    private int quantity;

    public FulfillmentOrderLineItemInput(int id, int quantity) {
        this.id = id;
        this.add(quantity);
    }

    public void add(int quantity) {
        this.quantity += quantity;
    }
}
