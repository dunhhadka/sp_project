package org.example.product.product.domain.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.product.domain.product.model.ProductId;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "inventory_items")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class InventoryItem {
    @Id
    private int id;
    private int storeId;
    private int productId;
    private int variantId;
    @Size(max = 50)
    private String sku;
    @Size(max = 200)
    private String barcode;
    private boolean tracked;
    private boolean requireShipping;
    @NotNull
    private BigDecimal costPrice;
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant modifiedAt;

    public InventoryItem(
            Integer id,
            ProductId productId,
            Integer variantId,
            String sku,
            String barcode,
            boolean tracked,
            boolean requireShipping,
            BigDecimal costPrice
    ) {
        this.id = id;
        this.productId = productId.getId();
        this.storeId = productId.getStoreId();
        this.variantId = variantId;
        this.sku = sku;
        this.barcode = barcode;
        this.tracked = tracked;
        this.requireShipping = requireShipping;
        this.costPrice = costPrice;
        this.createdAt = Instant.now();
        this.modifiedAt = Instant.now();
    }
}
