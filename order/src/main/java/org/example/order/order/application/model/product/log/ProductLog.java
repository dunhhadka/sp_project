package org.example.order.order.application.model.product.log;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductLog {
    private int id;
    private int productId;
    private String verb;
    private String data;
}
