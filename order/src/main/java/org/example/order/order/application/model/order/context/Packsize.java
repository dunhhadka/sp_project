package org.example.order.order.application.model.order.context;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class Packsize {
    private int id;
    private int variantId;
    private int productId;
    private int storeId;
    private int packsizeVariantId;
    private int packsizeProductId;
    private BigDecimal quantity;

    private Instant modifiedOn;
    private Instant createdOn;
}
