package org.example.order.order.application.service.fulfillmentorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.model.fulfillmentorder.request.InventoryLevel;
import org.example.order.order.application.model.fulfillmentorder.response.LocationForMove;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderLineItem;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.infrastructure.data.dto.Location;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentOrderHelperService {

    private static final int FETCH_SIZE = 250;

    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final InventoryClientService inventoryClientService;
    private final LocationClientService locationClientService;

    private final MessageSource messageSource;

    public List<LocationForMove> getMovableLocations(FulfillmentOrderId fulfillmentOrderId) {
        var storeId = fulfillmentOrderId.getStoreId();
        var fulfillmentOrder = fulfillmentOrderRepository.findById(fulfillmentOrderId)
                .orElseThrow(() -> {
                    log.warn("Can't find fulfillment order with id = {}", fulfillmentOrderId);
                    return new NotFoundException();
                });

        // get all inventory levels of fulfillment order
        var inventoryItemIds = fulfillmentOrder.getLineItems().stream()
                .map(FulfillmentOrderLineItem::getInventoryItemId)
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();
        List<InventoryLevel> inventoryLevels = inventoryClientService.getAllInventoryLevelsByInventoryItemIds(storeId, inventoryItemIds);

        // get all location available for inventory management.
        var locationFilter = LocationFilter.builder()
                .inventoryManagement(true)
                .limit(FETCH_SIZE)
                .build();
        Map<Long, Location> locationMap = locationClientService.getAllLocations(storeId, locationFilter).stream()
                .collect(Collectors.toMap(
                        Location::getId,
                        Function.identity()));

        // map theo key: locationId, value: map<Integer, Integer> inventoryItemQuantity,
        Map<Integer, Map<Integer, Integer>> locationInventoryQuantity = inventoryLevels.stream()
                .collect(Collectors.groupingBy(
                        InventoryLevel::getLocationId,
                        Collectors.groupingBy(
                                InventoryLevel::getInventoryItemId,
                                Collectors.reducing(
                                        0,
                                        line -> line.getAvailable().intValue(),
                                        Integer::sum
                                )
                        )
                ));


        return locationMap.entrySet().stream()
                .map(entry -> {
                    boolean movable = true;
                    String message = "";
                    int locationId = Math.toIntExact(entry.getKey());
                    Location locationDetail = entry.getValue();
                    var location = LocationForMove.Location.builder()
                            .id(locationId)
                            .name(locationDetail.getName())
                            .build();
                    if (Objects.equals(fulfillmentOrder.getAssignedLocationId(), (long) locationId)) {
                        movable = false;
                        message = messageSource.getMessage("locations_for_move.error.current_location", null, LocaleContextHolder.getLocale());
                    } else if (!inventoryItemIds.isEmpty()) {
                        Map<Integer, Integer> inventoryItemQuantity = locationInventoryQuantity.get(locationId);
                        if (inventoryItemQuantity == null) {
                            movable = false;
                            message = messageSource.getMessage("locations_for_move.error.no_items", null, LocaleContextHolder.getLocale());
                        } else {
                            List<Integer> itemNotStockedAtLocation = inventoryItemIds.stream()
                                    .filter(id -> !inventoryItemQuantity.containsKey(id))
                                    .toList();
                            if (!itemNotStockedAtLocation.isEmpty()) {
                                movable = false;
                                if (itemNotStockedAtLocation.size() == 1) {
                                    var inventoryItemId = itemNotStockedAtLocation.get(0);
                                    message = messageSource.getMessage("", null, LocaleContextHolder.getLocale());
                                } else {
                                    message = messageSource.getMessage("", null, LocaleContextHolder.getLocale());
                                }
                            }
                        }
                    }

                    return LocationForMove.builder()
                            .location(location)
                            .message(message)
                            .movable(movable)
                            .build();
                })
                .toList();
    }

}