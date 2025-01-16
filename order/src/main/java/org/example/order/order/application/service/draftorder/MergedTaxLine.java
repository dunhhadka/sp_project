package org.example.order.order.application.service.draftorder;


import org.example.order.order.application.service.orderedit.GenericTaxLine;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.TaxSettingValue;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class MergedTaxLine implements GenericTaxLine {
    private final String title;
    private final BigDecimal rate;
    private final boolean isCustom;
    private BigDecimal price = BigDecimal.ZERO;
    private int quantity = 0;

    public MergedTaxLine(TaxLineKey taxLineKey) {
        this.title = taxLineKey.title;
        this.rate = taxLineKey.rate;
        this.isCustom = taxLineKey.isCustom;
    }


    public static Collector<GenericTaxLine, Map<TaxLineKey, MergedTaxLine>, Map<TaxLineKey, MergedTaxLine>> merge() {
        return Collector.of(
                HashMap::new,
                MergedTaxLine::merge,
                MergedTaxLine::throwOnParallel
        );
    }

    public static Map<TaxLineKey, CombinationCalculateResponse.ComboPacksizeTaxLine> toValue(Map<TaxLineKey, MergedTaxLine> mergedTaxLineMap) {
        return mergedTaxLineMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new CombinationCalculateResponse.ComboPacksizeTaxLine(entry.getValue())
                ));
    }


    private static <T> T throwOnParallel(T t1, T t2) {
        throw new IllegalArgumentException();
    }

    public static void merge(Map<TaxLineKey, MergedTaxLine> map, GenericTaxLine taxLine) {
        map.compute(
                TaxLineKey.from(taxLine),
                (key, value) -> value == null
                        ? new MergedTaxLine(TaxLineKey.from(taxLine)).add(taxLine)
                        : value.add(taxLine)
        );
    }

    public static <T extends GenericTaxLine> BigDecimal distribute(T taxLine, BigDecimal itemPrice, BigDecimal totalLinePrice, Currency currency) {
        var taxLinePrice = taxLine.getPrice();
        if (!NumberUtils.isPositive(taxLinePrice) || !NumberUtils.isPositive(totalLinePrice)) return BigDecimal.ZERO;
        return taxLinePrice.multiply(itemPrice).divide(totalLinePrice, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
    }

    public static CombinationCalculateResponse.ComboPacksizeTaxLine buildComboPacksizeTaxLine(
            TaxSettingValue productTax,
            TaxSettingValue countryTax,
            BigDecimal linePrice,
            boolean taxIncluded,
            Currency currency
    ) {
        MergedTaxLine meredTaxLine = buildTaxLine(productTax, countryTax, linePrice, taxIncluded, currency);
        return new CombinationCalculateResponse.ComboPacksizeTaxLine(meredTaxLine);
    }

    public static MergedTaxLine buildTaxLine(
            TaxSettingValue productTax,
            TaxSettingValue countryTax,
            BigDecimal linePrice,
            boolean taxIncluded,
            Currency currency
    ) {
        if (productTax != null) {
            return buildTaxLine(productTax, linePrice, currency, taxIncluded);
        }
        return buildTaxLine(countryTax, linePrice, currency, taxIncluded);
    }

    private static MergedTaxLine buildTaxLine(
            TaxSettingValue taxSettingValue,
            BigDecimal linePrice,
            Currency currency,
            boolean taxIncluded
    ) {
        var rate = taxSettingValue.getRate();
        var taxAmount = getTaxAmount(rate, linePrice, currency, taxIncluded);
        return new MergedTaxLine(new TaxLineKey(taxSettingValue.getTitle(), rate, false))
                .addPrice(taxAmount);
    }

    private static BigDecimal getTaxAmount(BigDecimal rate, BigDecimal linePrice, Currency currency, boolean taxIncluded) {
        var price = linePrice.multiply(rate);
        if (taxIncluded)
            return price.divide(BigDecimal.ONE.add(rate), currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return price.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    private MergedTaxLine add(GenericTaxLine taxLine) {
        this.addPrice(taxLine.getPrice());
        // Tăng quantity trong mergedTaxLine
        this.quantity += taxLine.getQuantity();

        return this;
    }

    private MergedTaxLine addPrice(BigDecimal price) {
        if (NumberUtils.isPositive(price)) {
            this.price = this.price.add(price);
        }
        return this;
    }

    public record TaxLineKey(@NotBlank String title, BigDecimal rate, boolean isCustom) {
        public static TaxLineKey from(GenericTaxLine taxLine) {
            return new TaxLineKey(taxLine.getTitle(), taxLine.getRate(), taxLine.isCustom());
        }
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public BigDecimal getRate() {
        return this.rate;
    }

    @Override
    public BigDecimal getPrice() {
        return this.price;
    }

    @Override
    public boolean isCustom() {
        return this.isCustom;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }
}
