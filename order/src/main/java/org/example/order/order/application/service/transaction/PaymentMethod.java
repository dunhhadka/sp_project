package org.example.order.order.application.service.transaction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentMethod {
    private int id;

    private int providerId;

    private String name;

    private String description;

    private String status;
}
