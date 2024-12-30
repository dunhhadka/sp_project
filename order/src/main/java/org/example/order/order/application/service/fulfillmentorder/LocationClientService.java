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

    private final SapoClient sapoClient;

    public List<Location> getAllLocations(int storeId, LocationFilter locationFilter) {
        List<Location> locations = new ArrayList<>();
        boolean hasMoreResult = true;
        var limit = locationFilter.getLimit();

        while (hasMoreResult) {
            var locationPage = sapoClient.locationList(locationFilter);
            locations.addAll(locationPage);
            hasMoreResult = locationPage.size() == limit;
            locationFilter = locationFilter.toBuilder()
                    .page(locationFilter.getPage() + 1)
                    .limit(limit)
                    .build();
        }

        return locations.stream()
                .collect(Collectors.toMap(
                        Location::getId,
                        Function.identity(),
                        (l1, l2) -> l1)
                )
                .values().stream()
                .toList();
    }
}