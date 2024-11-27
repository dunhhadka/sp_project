package org.example.product.product.domain.product.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PropertyChanged extends DomainEventBase {

    private int id;

    private List<ObjectPropertyChange> changes;

    @Override
    public String eventName() {
        return this.getClass().getSimpleName();
    }
}
