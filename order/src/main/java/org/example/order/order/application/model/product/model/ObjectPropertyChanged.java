package org.example.order.order.application.model.product.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ObjectPropertyChanged {
    private String propertyName;
    private Object oldValue;
    private Object currentValue;
}
