package org.example.order.order.application.service.fulfillmentorder;

import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.SapoClient;
import org.example.order.order.application.model.fulfillmentorder.request.InventoryLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryClientService {

    private static final int CHUNK_SIZE = 100;
    private static final int FETCH_SIZE = 250;

    private final SapoClient sapoClient;

    public List<InventoryLevel> getAllInventoryLevelByInventoryItems(int storeId, List<Integer> inventoryItemIds) {
        List<List<Integer>> inventoryItemIdsChunks = Lists.partition(inventoryItemIds, CHUNK_SIZE); // chia theo thành các đoạn, mỗi đoạn có CHUNK_SIZE phần tử

        List<CompletableFuture<List<InventoryLevel>>> fetchAllInventoryLevelsFutures = new ArrayList<>();
        for (var chunk : inventoryItemIdsChunks) {
            CompletableFuture<List<InventoryLevel>> inventoryLevelsFutures = getInventoryLevelsByInventoryItemIdsFuture(storeId, chunk);
            fetchAllInventoryLevelsFutures.add(inventoryLevelsFutures);
        }

        return fetchAllInventoryLevelsFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(InventoryLevel::getId, Function.identity(), (a, b) -> a))
                .values().stream()
                .toList();
    }

    private CompletableFuture<List<InventoryLevel>> getInventoryLevelsByInventoryItemIdsFuture(int storeId, List<Integer> inventoryItemIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<InventoryLevel> inventoryLevels = new ArrayList<>();
            int page = 1;
            boolean hasMoreResults = true;
            while (hasMoreResults) {
                var inventoryLevelFilter = InventoryLevelFilterRequest.builder()
                        .inventoryItemIds(inventoryItemIds)
                        .limit(FETCH_SIZE)
                        .page(page)
                        .build();
                var inventoryLevelsPage = sapoClient.inventoryLevels(inventoryLevelFilter);
                inventoryLevels.addAll(inventoryLevelsPage);
                hasMoreResults = FETCH_SIZE == inventoryLevelsPage.size();
                page++;
            }
            return inventoryLevels;
        });
    }

    @Getter
    @Builder
    public static class InventoryLevelFilterRequest {
        private List<Integer> inventoryItemIds;
        private int limit;
        private int page;
    }
}
