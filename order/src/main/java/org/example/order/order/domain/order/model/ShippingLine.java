package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Where;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "shipping_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingLine {
    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order aggRoot;

    @Id
    private int id;

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotNull
    private BigDecimal price;

    @Size(max = 100)
    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "targetId", referencedColumnName = "id", updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Where(clause = "targetType = 'shipping_line'")
    @OrderBy("id asc")
    private List<@Valid DiscountAllocation> discountAllocations;

    @Size(max = 100)
    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "targetId", referencedColumnName = "id", updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Where(clause = "targetType = 'shipping_line'")
    @OrderBy("id asc")
    private List<@Valid TaxLine> taxLines;

    public ShippingLine(int id, String code, String title, String source, BigDecimal price) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.price = price;
        if (StringUtils.isBlank(this.code)) {
            this.code = this.title;
        }
    }

    public void applyTax(List<TaxLine> taxLines) {
        this.taxLines = taxLines;
    }

    public void allocateDiscount(DiscountAllocation discountAllocation) {
        this.discountAllocations.add(discountAllocation);
    }

    @JsonIgnore
    public BigDecimal getDiscountedTotal() {
        if (CollectionUtils.isEmpty(this.discountAllocations)) return price;
        var totalDiscount = this.discountAllocations.stream()
                .map(DiscountAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return price.subtract(totalDiscount);
    }
}
