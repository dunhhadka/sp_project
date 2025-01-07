package org.example.order.order.application.service.orderedit;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;

public class MergedTaxLine implements GenericTaxLine {

    private final TaxLineKey key;
    private BigDecimal price = BigDecimal.ZERO;
    private int quantity;

    public MergedTaxLine(TaxLineKey key) {
        this.key = key;
    }

    public static Collector<GenericTaxLine, ?, Map<TaxLineKey, MergedTaxLine>> toMergeMap() {
        return Collector.of(
                HashMap::new,
                MergedTaxLine::merge,
                MergedTaxLine::throwOnParallel
        );
    }

    private static <T> T throwOnParallel(T t1, T t2) {
        throw new UnsupportedOperationException();
    }

    private static void merge(Map<TaxLineKey, MergedTaxLine> map, GenericTaxLine taxLine) {
        map.compute(
                getKey(taxLine),
                (key, merged) -> (merged == null ? new MergedTaxLine(key) : merged.merge(taxLine))
        );
    }

    private MergedTaxLine merge(GenericTaxLine taxLine) {
        addPrice(taxLine.getPrice());

        quantity += taxLine.getQuantity();

        return this;
    }

    private void addPrice(BigDecimal otherPrice) {
        if (otherPrice.signum() != 0) {
            price = price.add(otherPrice);
        }
    }

    private static TaxLineKey getKey(GenericTaxLine taxLine) {
        return taxLine instanceof MergedTaxLine mtl
                ? mtl.key
                : TaxLineKey.from(taxLine);
    }

    public record TaxLineKey(String title, BigDecimal rate, boolean custom) {
        public static TaxLineKey from(GenericTaxLine taxLine) {
            return new TaxLineKey(taxLine.getTitle(), taxLine.getRate(), taxLine.isCustom());
        }
    }

    @Override
    public String getTitle() {
        return key.title;
    }

    @Override
    public BigDecimal getRate() {
        return key.rate;
    }

    @Override
    public BigDecimal getPrice() {
        return this.price;
    }

    @Override
    public boolean isCustom() {
        return key.custom;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }
}
