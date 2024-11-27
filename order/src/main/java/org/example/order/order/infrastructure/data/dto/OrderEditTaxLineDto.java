package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.service.orderedit.GenerateTaxLine;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class OrderEditTaxLineDto implements GenerateTaxLine {
    private String id;
    private int storeId;
    private int editingId;
    private boolean custom;

    private String title;
    private BigDecimal rate; // eg. 0.1, 0.2
    private BigDecimal price; // calculated value

    private String lineItemId;
    private int quantity; // line_item quantity
    // ---
    private Instant updatedAt;
    private Integer version;
}
