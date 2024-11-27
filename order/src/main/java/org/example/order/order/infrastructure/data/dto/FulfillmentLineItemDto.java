package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FulfillmentLineItemDto {
    private Integer id;
    private Integer storeId;
    private Integer orderId;
    private Integer fulfillmentId;
    private Integer quantity;
    private Integer effectiveQuantity;
    private Long lineItemId;
}
