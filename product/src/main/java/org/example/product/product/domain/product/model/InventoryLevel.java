package org.example.product.product.domain.product.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Table(name = "InventoryLevels")
public class InventoryLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int storeId;
    private int productId;
    private int variantId;
    private int inventoryItemId;
    private long locationId;
    private BigDecimal onHand = BigDecimal.ZERO;
    private BigDecimal available = BigDecimal.ZERO;
    private BigDecimal committed = BigDecimal.ZERO;
    private BigDecimal incoming = BigDecimal.ZERO;

    private Instant createAt;
    private Instant updateAt;

    public InventoryLevel(
            int storeId,
            int productId,
            int variantId,
            int inventoryItemId,
            int locationId,
            BigDecimal onHand,
            BigDecimal available,
            BigDecimal committed,
            BigDecimal incoming
    ) {
        this.storeId = storeId;
        this.productId = productId;
        this.variantId = variantId;
        this.inventoryItemId = inventoryItemId;
        this.locationId = locationId;
        this.onHand = onHand;
        this.available = available;
        this.committed = committed;
        this.incoming = incoming;

        this.createAt = Instant.now();
        this.updateAt = Instant.now();
    }
}
