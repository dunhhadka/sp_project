package org.example.product.product.application;

import org.example.product.product.infrastructure.data.dto.Location;

import java.util.List;

public interface SapoClient {
    List<Location> locations(List<Integer> locationIds, int storeId);
}
