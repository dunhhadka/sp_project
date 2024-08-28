package org.example.order.order.application.model.order.context;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCustomerContext {
    private String email;
    private String phone;
    private Integer customerId;
    private boolean acceptsMarketing;
    private boolean firstTimeCustomer;
}
