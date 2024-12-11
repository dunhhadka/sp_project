package org.example.order.order.application.service.orderedit;

import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import org.example.order.SapoClient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.Location;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCommitService {

    private final SapoClient sapoClient;

    private final AddService addService;
    private final ChangeQuantityService changeQuantityService;
    private final TaxService taxService;

    public void commit(Order order, OrderEdit orderEdit) {
        OrderEditUtils.GroupedStagedChange changes = OrderEditUtils.groupChanges(orderEdit.getStagedChanges());

        // validate location
        Map<Long, Location> locations = getLocations(changes.getAddItemActions().toList());

        List<LineItem> newLineItems = addService.addLineItems(order, orderEdit, changes);

        List<Pair<LineItem, Integer>> increaseLineItems = changeQuantityService.increaseQuantity(order, changes.incrementItems());

        List<ChangedLineItem> changedLineItems = increaseLineItems.stream()
                .map(increment -> new ChangedLineItem(increment.getFirst(), increment.getSecond()))
                .toList();
        taxService.addTaxForLineItems(order, newLineItems, changedLineItems);


    }

    public record ChangedLineItem(LineItem lineItem, int delta) {
    }

    private Map<Long, Location> getLocations(List<OrderStagedChange.AddLineItemAction> addActions) {
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
        Map<Long, Location> locations = sapoClient.locationList(filter.build()).stream()
                .collect(Collectors.toMap(
                        Location::getId,
                        Function.identity()));

        String locationIdNotFound = locationIds.stream()
                .filter(id -> !locations.containsKey(id))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        if (!locationIdNotFound.isEmpty()) {
            throw new ConstrainViolationException("location_ids", locationIdNotFound);
        }

        return locations;
    }
}
