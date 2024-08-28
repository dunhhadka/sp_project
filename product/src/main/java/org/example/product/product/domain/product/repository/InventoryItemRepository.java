package org.example.product.product.domain.product.repository;

import org.example.product.product.domain.product.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Integer> {

    List<InventoryItem> getByStoreIdAndVariantIdIn(int storeId, List<Integer> variantIds);

}
