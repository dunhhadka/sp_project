package org.example.order.order.application.service.customer;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Customer {
    private int id;
    private String email;
    private String phone;
    private boolean acceptsMarketing;
    private int ordersCount;
    private BigDecimal totalSpent;
}
