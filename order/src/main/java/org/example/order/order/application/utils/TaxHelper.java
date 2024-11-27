package org.example.order.order.application.utils;


import java.util.Set;

public interface TaxHelper {
    TaxSetting getTaxSetting(int storeId, String countryCode, Set<Integer> productIds);
}
