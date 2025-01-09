package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.CalculatedTaxLine;
import org.example.order.order.infrastructure.data.dto.RefundTaxLineDto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

public final class MergedTaxLine implements GenericTaxLine {

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

    public static Collector<Map<TaxLineKey, MergedTaxLine>, Map<TaxLineKey, MergedTaxLine>, List<CalculatedTaxLine>> mergerMaps() {
        return Collector.of(
                HashMap::new,
                MergedTaxLine::merge,
                MergedTaxLine::throwOnParallel,
                mergedMap -> mergedMap.values().stream().map(CalculatedTaxLine::new).toList()
        );
    }

    private static void merge(Map<TaxLineKey, MergedTaxLine> taxLineMap, Map<TaxLineKey, MergedTaxLine> taxLines) {
        taxLines.forEach((key1, value) -> merge(taxLineMap, value));
    }


    public MergedTaxLine merge(GenericTaxLine taxLine) {
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

    public MergedTaxLine mergeAll(List<RefundTaxLineDto> refundedTaxLines) {
        for (var refund : refundedTaxLines) merge(refund);

        return this;
    }

    private void merge(RefundTaxLineDto refund) {
        addPrice(refund.getAmount().negate());
    }

    public void reduce(int amount) {
        quantity -= amount;

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
