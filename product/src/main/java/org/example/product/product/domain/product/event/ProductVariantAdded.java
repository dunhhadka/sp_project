package org.example.product.product.domain.product.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.product.domain.product.model.Variant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantAdded extends DomainEventBase {

    private int variantId;
    private Variant variant;

    @Override
    public String eventName() {
        return getClass().getSimpleName();
    }
}
