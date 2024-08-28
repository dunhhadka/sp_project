package org.example.order.order.application.model.product.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductPayloadKafkaConnectLog extends ProductDto {
    private List<BaseEventLog> events;
}
