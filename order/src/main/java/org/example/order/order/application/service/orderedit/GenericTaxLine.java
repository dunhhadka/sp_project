package org.example.order.order.application.service.orderedit;

import java.math.BigDecimal;

public interface GenericTaxLine {
    String getTitle();

    BigDecimal getRate();

    BigDecimal getPrice();

    boolean isCustom();

    int getQuantity();
}
