package org.example.order.order.application.model.product.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BaseEventLog {
    private String  eventName;
    private String type;
    private int id;
    private String src;
    private List<ObjectPropertyChanged> changes;
}
