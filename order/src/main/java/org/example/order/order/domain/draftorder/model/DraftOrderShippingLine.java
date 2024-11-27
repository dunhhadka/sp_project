package org.example.order.order.domain.draftorder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DraftOrderShippingLine {

    @Size(max = 150)
    @NotBlank
    private String title;

    @Size(max = 255)
    private String alias;

    @Min(0)
    @NotNull
    private BigDecimal price;

    private boolean custom;
    @Size(max = 50)
    private String source;

    @Builder.Default
    private List<DraftTaxLine> taxLines = new ArrayList<>();

    public void addTax(DraftTaxLine taxLine) {
        if (taxLines == null) taxLines = new ArrayList<>();
        taxLines.add(taxLine);
    }

    public void removeTax() {
        this.taxLines = new ArrayList<>();
    }
}
