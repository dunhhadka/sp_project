package org.example.order.order.domain.orderedit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@DynamicUpdate
@NoArgsConstructor
@Table(name = "order_edit_line_items")
public class AddedLineItem {
    @Setter(AccessLevel.PACKAGE)
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "store_id", referencedColumnName = "store_id")
    @JoinColumn(name = "editing_id", referencedColumnName = "id")
    private OrderEdit aggRoot;

    @Id
    private UUID id;

    @Min(0)
    private Integer variantId;
    @Min(0)
    private Integer productId;

    private Integer locationId;

    @Size(max = 50)
    private String sku;
    @NotBlank
    @Size(max = 500)
    private String title;
    @Size(max = 500)
    private String variantTitle;

    private boolean taxable;
    private boolean requireShipping;

    private boolean restockable;

    @NotNull
    private BigDecimal editableQuantity;

    @NotNull
    private BigDecimal originalUnitPrice;
    @NotNull
    private BigDecimal discountedUnitPrice;
    @NotNull
    private BigDecimal editableSubtotal;

    private boolean hasStagedDiscount;

    @NotNull
    private Instant createdAt;
    @NotNull
    private Instant updatedAt;
    @Version
    private int version;

    public AddedLineItem(
            BigDecimal quantity,
            Integer variantId,
            Integer productId,
            Integer locationId,
            String sku,
            String title,
            String variantTitle,
            BigDecimal price,
            boolean taxable,
            boolean requiresShipping,
            boolean restockable
    ) {
        this.id = UUID.randomUUID();

        this.editableQuantity = quantity;

        this.variantId = variantId;
        this.productId = productId;
        this.locationId = locationId;

        this.sku = sku;
        this.title = title;
        this.variantTitle = variantTitle;

        this.originalUnitPrice = price;
        this.discountedUnitPrice = price;
        calculatePrice();
        applyDiscount();

        this.taxable = taxable;
        this.requireShipping = requiresShipping;
        this.restockable = restockable;

        this.createdAt = Instant.now();
    }

    private void applyDiscount() {
        this.hasStagedDiscount = this.discountedUnitPrice.compareTo(originalUnitPrice) < 0;
    }

    private void calculatePrice() {
        this.editableSubtotal = this.editableQuantity.multiply(this.discountedUnitPrice);
    }

    public void updateQuantity(BigDecimal quantity) {
        this.editableQuantity = quantity;
        this.calculatePrice();
    }

    public BigDecimal getTotalDiscount() {
        BigDecimal discountAmount = this.originalUnitPrice.subtract(this.discountedUnitPrice);
        if (discountAmount.signum() == 0) return BigDecimal.ZERO;
        return discountAmount.multiply(editableQuantity);
    }

    public void removeDiscount() {
        this.discountedUnitPrice = this.originalUnitPrice;
        this.hasStagedDiscount = false;
        this.calculatePrice();
    }

    public void applyDiscount(BigDecimal amount) {
        this.discountedUnitPrice = this.originalUnitPrice.subtract(amount);
        this.hasStagedDiscount = true;
        this.calculatePrice();
        this.updatedAt = Instant.now();
    }
}
