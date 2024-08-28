package org.example.product.product.domain.product.repository;

import org.example.product.product.domain.product.model.InventoryLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Integer> {
    List<InventoryLevel> findByStoreIdAndInventoryItemIdIn(int storeId, List<Integer> inventoryItemIds);
}

