package org.example.order.order.domain.orderedit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Getter
@Entity
@DynamicUpdate
@Table(name = "order_edit_tax_lines")
@NoArgsConstructor
public class AddedTaxLine {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "editing_id", referencedColumnName = "id")
    @JoinColumn(name = "store_id", referencedColumnName = "store_id")
    private OrderEdit aggRoot;

    @Id
    private UUID id;

    @NotBlank
    @Size(max = 255)
    private String title;

    private BigDecimal rate;

    @NotNull
    private BigDecimal price;

    private String lineItemId;

    private BigDecimal quantity; // đối với line cũ nếu tăng số lượng thì có thể apply thuế

    @NotNull
    private Instant createdAt;
    private Instant updateAt;
    @Version
    private Integer version;

    public AddedTaxLine(
            UUID id,
            String title,
            BigDecimal rate,
            AddedLineItem lineItem,
            Currency currency,
            boolean taxIncluded
    ) {
        this.id = id;

        this.title = title;
        this.rate = rate;

        this.lineItemId = lineItem.getId().toString();
        this.quantity = lineItem.getEditableQuantity();

        this.price = calculatePrice(taxIncluded, currency, lineItem.getEditableSubtotal());
    }

    private BigDecimal calculatePrice(boolean taxIncluded, Currency currency, BigDecimal subtotal) {
        var amount = subtotal.multiply(this.rate);
        if (taxIncluded) {
            var ratio = BigDecimal.ONE.add(this.rate);
            amount = amount.divide(ratio, currency.getDefaultFractionDigits(), RoundingMode.CEILING);
        } else {
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.CEILING);
        }
        return amount;
    }

    public BigDecimal updateQuantity(AddedLineItem lineItem, Currency currency, boolean taxIncluded) {
        this.quantity = lineItem.getEditableQuantity();
        this.price = calculatePrice(taxIncluded, currency, lineItem.getEditableSubtotal());
        this.updateAt = Instant.now();
        return this.price;
    }
}
