package org.example.order.order.application.service.orderedit;

import org.example.order.order.infrastructure.data.dto.RefundTaxLineDto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

public class MergedTaxLine implements GenerateTaxLine {

    private final TaxLineKey key;

    private int quantity;
    private BigDecimal price;

    public MergedTaxLine(GenerateTaxLine taxLine) {
        this.key = TaxLineKey.from(taxLine);
    }

    public static Collector<GenerateTaxLine, Map<TaxLineKey, MergedTaxLine>, Map<TaxLineKey, MergedTaxLine>> toMergedMap() {
        return Collector.of(
                HashMap::new,
                MergedTaxLine::merge,
                MergedTaxLine::throwOnParallel
        );
    }

    private static Map<TaxLineKey, MergedTaxLine> throwOnParallel(Map<TaxLineKey, MergedTaxLine> taxLineKeyMergedTaxLineMap, Map<TaxLineKey, MergedTaxLine> taxLineKeyMergedTaxLineMap1) {
        throw new IllegalArgumentException("unsupported for parallelization");
    }

    private static void merge(Map<TaxLineKey, MergedTaxLine> map, GenerateTaxLine taxLine) {
        map.compute(
                getKey(taxLine),
                (key, merged) -> (merged != null ? merged.merge(taxLine) : new MergedTaxLine(taxLine).merge(taxLine))
        );
    }

    public static Collector<? super Map<TaxLineKey, MergedTaxLine>, Object, Object> mergeMaps() {
        return null;
    }


    public MergedTaxLine merge(GenerateTaxLine taxLine) {
        addPrice(taxLine.getPrice());
        this.quantity += taxLine.getQuantity();
        return this;
    }

    private void addPrice(BigDecimal price) {
        if (this.price == null) this.price = BigDecimal.ZERO;
        this.price = this.price.add(price);
    }

    private static TaxLineKey getKey(GenerateTaxLine taxLine) {
        return TaxLineKey.from(taxLine);
    }

    public MergedTaxLine mergeRefunds(List<RefundTaxLineDto> refundTaxLines) {
        for (var refund : refundTaxLines) merge(refund);
        return this;
    }

    private void merge(RefundTaxLineDto refund) {
        addPrice(refund.getAmount().negate());
    }


    public record TaxLineKey(String title, BigDecimal rate, boolean custom) {
        public static TaxLineKey from(GenerateTaxLine taxLine) {
            return new TaxLineKey(taxLine.getTitle(), taxLine.getRate(), taxLine.isCustom());
        }
    }

    @Override
    public int getQuantity() {
        return this.quantity;
    }

    @Override
    public BigDecimal getPrice() {
        return this.price;
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
    public boolean isCustom() {
        return key.custom;
    }
}
