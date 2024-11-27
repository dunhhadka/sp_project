package org.example.order.order.application.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.order.order.domain.draftorder.model.DraftTaxLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaxLineUtils {


    public static DraftTaxLine buildTaxLine(TaxSettingValue taxValue, BigDecimal price, Currency currency, boolean taxesIncluded) {
        return DraftTaxLine.builder()
                .rate(taxValue.getRate())
                .title(taxValue.getTitle())
                .price(TaxLineUtils.getTaxAmount(price, taxValue.getRate(), currency, taxesIncluded))
                .ratePercentage(taxValue.getRate().add(BigDecimals.ONE_HUNDRED))
                .build();
    }

    private static BigDecimal getTaxAmount(BigDecimal price, BigDecimal rate, Currency currency, boolean taxesIncluded) {
        var amount = price.multiply(rate);
        if (taxesIncluded)
            return amount.divide(BigDecimal.ONE.add(rate), currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static BigDecimal calculatePrice(BigDecimal price, BigDecimal rate, boolean taxesIncluded, Currency currency) {
        return getTaxAmount(price, rate, currency, taxesIncluded);
    }

    public static void merTaxLines(Map<String, DraftTaxLine> taxLineMap, DraftTaxLine taxLine) {
        var taxLineTitle = taxLine.getTitle();
        if (taxLineMap.containsKey(taxLineTitle)) {
            var taxExisted = taxLineMap.get(taxLineTitle);
            var finalTaxLine = taxExisted.mergeTax(taxLine);
            taxLineMap.put(taxLineTitle, finalTaxLine);
        } else {
            taxLineMap.put(taxLineTitle, taxLine);
        }
    }
}
