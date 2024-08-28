package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "combination_lines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CombinationLine {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PROTECTED)
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    private Order aggRoot;

    @Id
    private int id;

    private long variantId;
    private long productId;

    private BigDecimal price;
    private BigDecimal quantity;

    private @Size(max = 320) String title;
    private @Size(max = 500) String variantTitle;
    private @Size(max = 850) String name;

    private @Size(max = 50) String sku;
    private @Size(max = 255) String vendor;
    private @Size(max = 50) String unit;
    private @Size(max = 50) String itemUnit;

    @Enumerated(EnumType.STRING)
    private Type type;

    public CombinationLine(
            int id,
            long variantId,
            long productId,
            BigDecimal price,
            BigDecimal quantity,
            String title,
            String variantTitle,
            String sku,
            String vendor,
            String unit,
            String itemUnit,
            Type type
    ) {
        this.id = id;
        this.variantId = variantId;
        this.productId = productId;
        this.price = price;
        this.quantity = quantity;
        this.initVariantGeneralInfo(title, variantTitle);
        this.sku = sku;
        this.vendor = vendor;
        this.unit = unit;
        this.itemUnit = itemUnit;
        this.type = type;
    }

    private void initVariantGeneralInfo(String title, String variantTitle) {
        this.title = title;
        this.variantTitle = variantTitle;
        StringBuilder nameBuilder = new StringBuilder(this.title);
        if (StringUtils.isNotBlank(this.variantTitle) && !StringUtils.equals("Default Title", this.variantTitle)) {
            nameBuilder.append(" - ").append(this.variantTitle);
        }
        this.name = nameBuilder.toString();
    }

    public enum Type {
        combo,
        packsize
    }
}
