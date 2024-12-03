package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.example.order.SapoClient;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.Location;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCommitService {

    private final SapoClient sapoClient;

    public void commit(Order order, OrderEdit orderEdit) {
        OrderEditUtils.GroupedStagedChange changes = OrderEditUtils.groupChanges(orderEdit.getStagedChanges());

        // validate location
        List<Location> locations = getLocations(changes.getAddItemActions().toList());
    }

    private List<Location> getLocations(List<OrderStagedChange.AddLineItemAction> addActions) {
        List<Long> locationIds = addActions.stream()
                .map(OrderStagedChange.AddLineItemAction::getLocationId)
                .map(Long::valueOf)
                .distinct()
                .toList();

        boolean includeDefault = addActions.size() != locationIds.size();
        LocationFilter.LocationFilterBuilder filter = LocationFilter.builder().locationIds(locationIds);
        if (includeDefault) {
            filter.defaultLocation(true);
        }
        List<Location> locations = sapoClient.locationList(filter.build());

        
    }
}
