package org.example.order.order.application.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TaxSetting {
    private boolean taxIncluded; // Tính thuế trên cả sản phẩm và phí vận chuyển
    private boolean taxShipping;

    private List<TaxSettingValue> taxes = new ArrayList<>();

    private Set<Integer> productIds = new HashSet<>();

    private TaxStatus status = TaxStatus.inactive;

    private String countryCode;

    public static TaxSetting defaultTax() {
        return new TaxSetting();
    }

    public enum TaxStatus {
        active,
        inactive
    }
}
