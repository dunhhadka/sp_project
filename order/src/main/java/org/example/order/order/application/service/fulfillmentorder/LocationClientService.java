package org.example.order.order.application.service.fulfillmentorder;

import lombok.RequiredArgsConstructor;
import org.example.order.SapoClient;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.infrastructure.data.dto.Location;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationClientService {

    private static final int FETCH_SIZE = 250;

    private final SapoClient sapoClient;

    public List<Location> getAllLocation(LocationFilter locationFilter) {
        List<Location> locations = new ArrayList<>();
        var limit = getLimit(locationFilter.getLimit());
        boolean hasMoreResults = true;
        while (hasMoreResults) {
            var locationsPage = sapoClient.locationList(locationFilter);
            locations.addAll(locationsPage);
            hasMoreResults = locationsPage.size() == locationFilter.getLimit();
            locationFilter = locationFilter.toBuilder()
                    .page(locationFilter.getPage() + 1)
                    .limit(limit)
                    .build();
        }
        return locations.stream()
                .collect(Collectors.toMap(Location::getId, Function.identity(), (l1, l2) -> l1))
                .values().stream()
                .toList();
    }

    private int getLimit(int limit) {
        if (limit <= 0) return FETCH_SIZE;
        return Math.min(FETCH_SIZE, limit);
    }
}
