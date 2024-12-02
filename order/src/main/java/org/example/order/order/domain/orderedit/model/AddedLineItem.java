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
            int variantId,
            int productId,
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

        this.variantId = variantId;
        this.productId = productId;
        this.locationId = locationId;

        this.sku = sku;
        this.title = title;
        this.variantTitle = variantTitle;

        this.taxable = taxable;
        this.requireShipping = requiresShipping;
        this.restockable = restockable;

        this.originalUnitPrice = price;
        this.discountedUnitPrice = price;

        this.editableQuantity = quantity;
        this.calculateSubtotal();

        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private void calculateSubtotal() {
        this.editableSubtotal = this.discountedUnitPrice
                .multiply(this.editableQuantity);
    }

    public BigDecimal adjustQuantity(int delta) { // may be dela < 0
        BigDecimal adjustmentQuantity = new BigDecimal(delta);
        this.editableQuantity = this.editableQuantity.add(adjustmentQuantity);

        this.calculatePrice();

        return adjustmentQuantity.multiply(discountedUnitPrice);
    }

    private void calculatePrice() {
        this.editableSubtotal = this.discountedUnitPrice.multiply(this.editableQuantity);
    }

    public BigDecimal adjustQuantity(BigDecimal editableQuantity) {
        BigDecimal priceBeforeChange = this.editableSubtotal;

        this.editableQuantity = editableQuantity;
        this.calculatePrice();

        this.updatedAt = Instant.now();

        return editableSubtotal.subtract(priceBeforeChange);
    }

    public BigDecimal getTotalDiscount() {
        BigDecimal totalDiscountedPrice = this.discountedUnitPrice
                .multiply(this.editableQuantity);
        return editableSubtotal.subtract(totalDiscountedPrice);
    }
}
