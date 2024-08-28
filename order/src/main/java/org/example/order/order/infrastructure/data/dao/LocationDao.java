package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.Location;

public interface LocationDao {
    Location getById(int storeId, int id);
}
