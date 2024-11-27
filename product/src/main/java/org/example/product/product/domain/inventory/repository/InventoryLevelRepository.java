package org.example.product.product.domain.inventory.repository;

import org.example.product.product.domain.inventory.InventoryLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Integer> {

}
