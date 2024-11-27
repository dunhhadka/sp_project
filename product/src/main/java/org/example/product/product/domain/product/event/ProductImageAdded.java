package org.example.product.product.domain.product.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageAdded extends DomainEventBase {

    private int imageId;
    private String fileName;
    private String src;

    @Override
    public String eventName() {
        return this.getClass().getSimpleName();
    }
}
