package org.example.order.order.domain.draftorder.persistence;

import org.example.order.order.domain.draftorder.model.DraftOrder;

public interface DraftOrderRepository {
    void store(DraftOrder draftOrder);
}
