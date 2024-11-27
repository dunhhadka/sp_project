package org.example.product.product.domain.product.event;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ProductTagAdded extends DomainEventBase {

    private String tagName;

    @Override
    public String eventName() {
        return this.getClass().getSimpleName();
    }
}
