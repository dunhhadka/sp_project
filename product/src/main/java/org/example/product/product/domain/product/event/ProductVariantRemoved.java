package org.example.product.product.domain.product.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRemoved extends DomainEventBase {
    private int variantId;

    @Override
    public String eventName() {
        return this.getClass().getSimpleName();
    }
}
