package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderStagedChangeDto {
    private UUID id;
    private int storeId;
    private int editingId;

    private OrderStagedChange.ChangeType type;
    private String value;

    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;
}
