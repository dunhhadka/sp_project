package org.example.order.order.domain.draftorder.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DraftTaxLine {
    private BigDecimal price;
    private BigDecimal rate;
    private BigDecimal ratePercentage;
    private String title;

    public DraftTaxLine mergeTax(DraftTaxLine taxLine) {
        this.price = this.price.add(taxLine.getPrice());
        return this;
    }
}
