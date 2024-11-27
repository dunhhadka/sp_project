package org.example.product.product.domain.product.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Triple;

@Getter
@NoArgsConstructor
public class ObjectPropertyChange {
    private String fieldName;
    private Object oldValue;
    private Object currentValue;

    public ObjectPropertyChange(Triple<String, Object, Object> diff) {
        this.fieldName = diff.getLeft();
        this.oldValue = diff.getMiddle();
        this.currentValue = diff.getRight();
    }
}
