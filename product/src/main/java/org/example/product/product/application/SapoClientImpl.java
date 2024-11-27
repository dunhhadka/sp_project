package org.example.product.product.application;

import lombok.RequiredArgsConstructor;
import org.example.product.product.infrastructure.data.dto.Location;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SapoClientImpl implements SapoClient {
    @Override
    public List<Location> locations(List<Integer> locationIds, int storeId) {
        return null;
    }
}
