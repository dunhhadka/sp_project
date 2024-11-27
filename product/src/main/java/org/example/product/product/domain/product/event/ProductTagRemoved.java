package org.example.product.product.domain.product.event;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.product.product.domain.product.model.Tag;

@NoArgsConstructor
@AllArgsConstructor
public class ProductTagRemoved extends DomainEventBase {

    private Tag tag;

    @Override
    public String eventName() {
        return this.getClass().getSimpleName();
    }
}
