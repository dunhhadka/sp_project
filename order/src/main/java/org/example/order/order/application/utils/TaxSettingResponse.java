package org.example.order.order.application.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TaxSettingResponse {

    private TaxSetting taxSetting;

    @Getter
    @Setter
    @Builder
    public static class TaxSetting {
        private int id;
        private int storeId;
        private String status;
        private boolean taxIncluded; // đã bao gồm thuế
        private boolean taxShipping; // có tính thuế ship hay không
    }
}
