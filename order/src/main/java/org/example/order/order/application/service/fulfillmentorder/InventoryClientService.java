package org.example.order.order.application.service.fulfillmentorder;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.SapoClient;
import org.example.order.order.application.model.fulfillmentorder.request.InventoryLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryClientService {

    private final SapoFetcher sapoInventoryFetcher;

    public List<InventoryLevel> getAllInventoryLevelsByInventoryItemIds(int storeId, List<Integer> inventoryItemIds) {
        if (CollectionUtils.isEmpty(inventoryItemIds)) {
            return Collections.emptyList();
        }

        List<List<Integer>> inventoryItemIdsChunk = Lists.partition(inventoryItemIds, 100);
        List<CompletableFuture<List<InventoryLevel>>> fetchFutures = inventoryItemIdsChunk.stream()
                .map(chunk -> sapoInventoryFetcher.fetchAllAsync(storeId, chunk))
                .toList();

        return fetchFutures.stream()
                .map(future -> future.exceptionally(ex -> {
                    log.error("Failed to fetch inventory levels: ", ex);
                    return Collections.emptyList();
                }))
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    @Service
    @RequiredArgsConstructor
    public static class SapoFetcher extends AbstractFetcher<InventoryLevel> {

        private final SapoClient sapoClient;

        @Override
        protected boolean hasMore(List<InventoryLevel> fetchedResult, List<Integer> chunk) {
            return fetchedResult.size() == chunk.size();
        }

        @Override
        protected List<InventoryLevel> fetch(int storeId, List<Integer> inventoryItemIds, int page) {
            InventoryLevelFilterRequest request = null;
            request.setStoreId(storeId);
            request.setFetchSize(250);
            request.setPage(page);
            request.setInventoryItemIds(inventoryItemIds);
            return sapoClient.inventoryLevels(request);
        }
    }

    @Setter
    @Getter
    public static class InventoryLevelFilterRequest {
        private int storeId;
        private int fetchSize;
        private int page;
        private List<Integer> inventoryItemIds;
    }

    abstract static class AbstractFetcher<T> {

        public List<T> fetchAll(int storeId, List<Integer> chunk) {
            List<T> results = new ArrayList<>();
            int page = 1;
            boolean hasMoreResult = true;

            while (hasMoreResult) {
                List<T> fetchedResult = fetch(storeId, chunk, page);
                results.addAll(fetchedResult);
                hasMoreResult = hasMore(fetchedResult, chunk);
                page++;
            }

            return results;
        }

        public CompletableFuture<List<T>> fetchAllAsync(int storeId, List<Integer> chunk) {
            return CompletableFuture.supplyAsync(() -> fetchAll(storeId, chunk));
        }

        protected abstract boolean hasMore(List<T> fetchedResult, List<Integer> chunk);

        protected abstract List<T> fetch(int storeId, List<Integer> itemIds, int page);
    }
}