package org.example.product.product.domain.product.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "InventoryItems")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {
    @Id
    private int id;

    private int storeId;
    private int productId;
    private int variantId;

    @Size(max = 50)
    private String sku;
    @Size(max = 50)
    private String barcode;

    private boolean tracked;
    private boolean requireShipping;

    private Instant createdAt;
    private Instant modifiedAt;

    @Transient
    private boolean isUpdate;

    public InventoryItem(
            int id,
            int storeId,
            int productId,
            int variantId,
            String sku,
            String barcode,
            boolean tracked,
            Boolean requireShipping
    ) {
        this.id = id;
        this.storeId = storeId;
        this.productId = productId;
        this.variantId = variantId;
        this.sku = sku;
        this.barcode = barcode;
        this.tracked = tracked;
        this.requireShipping = requireShipping;

        this.createdAt = Instant.now();
        this.modifiedAt = Instant.now();
    }
}
