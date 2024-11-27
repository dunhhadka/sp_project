package org.example.order.order.domain.orderedit.persistence;

import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.domain.orderedit.model.OrderEditId;

public interface OrderEditRepository {
    void save(OrderEdit orderEdit);

    OrderEdit findById(OrderEditId id);
}
