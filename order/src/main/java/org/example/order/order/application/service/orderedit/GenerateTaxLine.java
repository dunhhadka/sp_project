package org.example.order.order.application.service.orderedit;

import java.math.BigDecimal;

public interface GenerateTaxLine {
    String getTitle();

    BigDecimal getRate();

    boolean isCustom();

    default int getQuantity() {
        return 0;
    }

    default BigDecimal getPrice() {
        return BigDecimal.ZERO;
    }
}
