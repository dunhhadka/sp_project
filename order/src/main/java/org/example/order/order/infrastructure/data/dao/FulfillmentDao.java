package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.FulfillmentDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FulfillmentDao {

    List<FulfillmentDto> getByOrderIds(int storeId, List<Integer> orderId);

    CompletableFuture<List<FulfillmentDto>> getByStoreIdsAndOrderIdsAsync(List<Integer> storeIds, List<Integer> orderIds);
}
