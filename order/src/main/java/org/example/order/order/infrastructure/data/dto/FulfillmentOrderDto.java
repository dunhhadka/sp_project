package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;

import java.time.Instant;

@Getter
@Setter
public class FulfillmentOrderDto {
    private long id;
    private int storeId;
    private long orderId;

    private Long assignedLocationId;
    private FulfillmentOrder.InventoryBehaviour inventoryBehaviour;
    private FulfillmentOrder.FulfillmentOrderStatus status;

    private String requestStatus;
    private Boolean requiredShipping;
    private String expectedDeliveryMethod;

    private Instant fulfillOn;
    private Instant fulfillBy;

    private Instant createdOn;
    private String createdBy;
    private Instant modifiedOn;
    private String lastModifiedBy;

    private Integer version;
}
