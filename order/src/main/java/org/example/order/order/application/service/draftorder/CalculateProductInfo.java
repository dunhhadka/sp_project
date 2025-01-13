package org.example.order.order.application.service.draftorder;

import lombok.Builder;
import lombok.Getter;
import org.example.order.order.application.model.order.context.Combo;
import org.example.order.order.application.model.order.context.Packsize;
import org.example.order.order.application.model.order.context.Product;
import org.example.order.order.application.model.order.context.ProductVariant;
import org.example.order.order.application.utils.TaxSettingValue;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class CalculateProductInfo {
    @Builder.Default
    private Map<Integer, ProductVariant> variantMap = new HashMap<>();

    @Builder.Default
    private Map<Integer, Product> productMap = new HashMap<>();

    @Builder.Default
    private Map<Integer, Combo> comboMap = new HashMap<>();

    @Builder.Default
    private Map<Integer, Packsize> packsizeMap = new HashMap<>();

    @Builder.Default
    private Map<Integer, TaxSettingValue> productTaxMap = new HashMap<>();

    private TaxSettingValue countryTaxSetting;

    @Builder.Default
    private Currency currency = Currency.getInstance("VND");

    @Builder.Default
    private BigDecimal remainderUnit = BigDecimal.ONE;
}
