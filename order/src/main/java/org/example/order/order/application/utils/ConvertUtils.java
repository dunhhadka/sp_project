package org.example.order.order.application.utils;

public final class ConvertUtils {

    public static Integer toInt(Long number) {
        return number == null ? null : number.intValue();
    }
}
