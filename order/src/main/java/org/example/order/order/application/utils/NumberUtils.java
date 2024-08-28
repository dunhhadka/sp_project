package org.example.order.order.application.utils;

import java.math.BigDecimal;

public final class NumberUtils {
    public static boolean isPositive(Integer number) {
        return number != null && number > 0;
    }

    public static boolean isPositive(Long number) {
        return number != null && number > 0;
    }

    public static boolean isPositive(BigDecimal decimal) {
        return decimal != null && decimal.signum() > 0;
    }
}
