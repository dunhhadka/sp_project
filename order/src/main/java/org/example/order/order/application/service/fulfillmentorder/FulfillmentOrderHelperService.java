package org.example.order.order.application.service.fulfillmentorder;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.model.fulfillmentorder.request.InventoryLevel;
import org.example.order.order.application.model.fulfillmentorder.response.LocationForMove;
import org.example.order.order.application.model.order.request.LocationFilter;
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

@Service
@RequiredArgsConstructor
public class FulfillmentOrderHelperService {

    private static final int FETCH_SIZE = 250;

    private final FulfillmentOrderRepository fulfillmentOrderRepository;

    private final InventoryClientService inventoryClientService;
    private final LocationClientService locationClientService;

    private final MessageSource messageSource;

    public List<LocationForMove> getLocationForMove(FulfillmentOrderId fulfillmentOrderId) {
        var storeId = fulfillmentOrderId.getStoreId();
        var fulfillmentOrder = fulfillmentOrderRepository.findById(fulfillmentOrderId)
                .orElseThrow(() -> new NotFoundException("not found"));

        var inventoryItemIds = fulfillmentOrder.getLineItems().stream()
                .map(FulfillmentOrderLineItem::getInventoryItemId)
                .distinct()
                .toList();

        List<InventoryLevel> inventoryLevels = inventoryClientService.getAllInventoryLevelByInventoryItems(storeId, inventoryItemIds);

        var locationFilter = LocationFilter.builder()
                .inventoryManagement(true)
                .limit(FETCH_SIZE)
                .build();
        var locationMap = locationClientService.getAllLocation(locationFilter).stream()
                .collect(Collectors.toMap(Location::getId, Function.identity())); // maybe duplicate

        Map<Integer, Map<Integer, Integer>> locationInventoryItemQuantity = inventoryLevels.stream()
                .collect(Collectors.groupingBy(InventoryLevel::getLocationId,
                        Collectors.groupingBy(InventoryLevel::getInventoryItemId,
                                Collectors.reducing(
                                        0,
                                        lineItem -> lineItem.getAvailable().intValue(),
                                        Integer::sum
                                ))));

        return locationMap.entrySet().stream().map(entry -> {
                    boolean movable = true;
                    String message = "";
                    int locationId = entry.getKey().intValue();
                    Location locationDetail = entry.getValue();
                    var location = LocationForMove.Location.builder()
                            .id(locationId)
                            .name(locationDetail.getName())
                            .build();
                    if (locationId == fulfillmentOrder.getAssignedLocationId()) {
                        movable = false;
                        message = messageSource.getMessage("locations_for_move.error.current_location", null, "Current location.", LocaleContextHolder.getLocale());
                    } else {
                        Map<Integer, Integer> inventoryItemQuantityMap = locationInventoryItemQuantity.get(locationId);
                        if (inventoryItemQuantityMap == null) {
                            movable = false;
                            message = messageSource.getMessage("locations_for_move.error.no_items", null, "No inventory items at this location.", LocaleContextHolder.getLocale());
                        } else {
                            var itemNoStockedAtLocation = inventoryItemIds.stream()
                                    .filter(id -> !inventoryItemQuantityMap.containsKey(id))
                                    .toList();
                            if (!itemNoStockedAtLocation.isEmpty()) {
                                movable = false;
                                if (itemNoStockedAtLocation.size() == 1) {
                                    var inventoryItemId = itemNoStockedAtLocation.get(0);
                                    var itemTitle = fulfillmentOrder.getLineItems().stream()
                                            .filter(line -> Objects.equals(line.getInventoryItemId(), inventoryItemId))
                                            .map(line -> line.getVariantInfo().getTitle())
                                            .findFirst().orElse("");
                                    message = messageSource.getMessage("locations_for_move.error.is_not_stocked", new Object[]{itemTitle}, itemTitle + " can't be changed because it isn't stocked at this location.", LocaleContextHolder.getLocale());
                                } else {
                                    message = messageSource.getMessage("locations_for_move.error.are_not_stocked", new Object[]{itemNoStockedAtLocation.size()}, itemNoStockedAtLocation.size() + " items can't be changed because they aren't stocked at this location.", LocaleContextHolder.getLocale());
                                }
                            }
                        }
                    }

                    return LocationForMove.builder()
                            .location(location)
                            .movable(movable)
                            .message(message)
                            .build();
                })
                .toList();
    }
}
