package org.example.product.product.domain.inventory.repository;

import org.example.product.product.domain.inventory.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Integer> {

}
