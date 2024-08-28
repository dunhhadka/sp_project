package org.example.order.order.application.model.order.context;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProductImageCache {
    private int productId;
    private int variantId;
    private String imageUrl;
}
