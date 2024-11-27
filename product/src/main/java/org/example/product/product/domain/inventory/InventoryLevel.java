package org.example.product.product.domain.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.product.domain.product.model.ProductId;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Builder
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_levels")
public class InventoryLevel { // nếu tracked = true => get theo quantity else => 0
    @Id
    private int id;
    @Column(updatable = false)
    private int storeId;
    @Column(updatable = false)
    private int inventoryItemId;
    private int productId;
    private int variantId;
    private int locationId;
    @Builder.Default
    private BigDecimal onHand = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal available = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal committed = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal incoming = BigDecimal.ZERO;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

    @Version
    private int version;

    public InventoryLevel(
            Integer id,
            ProductId productId,
            int variantId,
            int inventoryItemId,
            int locationId,
            BigDecimal onHand,
            BigDecimal available,
            BigDecimal committed,
            BigDecimal incoming
    ) {
        this.id = id;
        this.productId = productId.getId();
        this.storeId = productId.getStoreId();
        this.variantId = variantId;
        this.inventoryItemId = inventoryItemId;
        this.locationId = locationId;
        this.onHand = onHand;
        this.available = available;
        this.committed = committed;
        this.incoming = incoming;

        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
