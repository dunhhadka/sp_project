package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.domain.draftorder.model.DraftOrderNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaDraftOrderNumberRepository extends JpaRepository<DraftOrderNumber, Integer> {
    Optional<DraftOrderNumber> findFirstByStoreId(int storeId);
}
