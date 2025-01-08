package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.CalculatedLineItem;
import org.example.order.order.application.model.orderedit.OrderStagedChangeModel;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.LineItemDto;
import org.example.order.order.infrastructure.data.dto.OrderEditLineItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractLineItemBuilder<T> implements BuilderSteps.BuildResult, BuilderSteps.Builder {
    private final CalculatedLineItem lineItem;
    private final T context;
    private final List<OrderStagedChangeModel> changes;

    protected AbstractLineItemBuilder(CalculatedLineItem lineItem, T context) {
        this.lineItem = lineItem;
        this.context = context;
        this.changes = new ArrayList<>();
    }

    public AbstractLineItemBuilder(OrderEditLineItemDto lineItem, T context) {
        this(new CalculatedLineItem(lineItem), context);
    }

    public AbstractLineItemBuilder(LineItemDto lineItem, T context) {
        this(new CalculatedLineItem(lineItem), context);
    }

    @Override
    public Map<MergedTaxLine.TaxLineKey, MergedTaxLine> taxLines() {
        return streamTaxLines().collect(MergedTaxLine.toMergeMap());
    }

    protected abstract Stream<? extends GenericTaxLine> streamTaxLines();

    @Override
    public BuilderSteps.BuildResult build() {
        doBuild();

        lineItem.setStagedChanges(changes);

        return this;
    }

    // apply context and change to lineItem
    protected abstract void doBuild();

    protected T context() {
        return this.context;
    }

    @Override
    public CalculatedLineItem lineItem() {
        return this.lineItem;
    }

    protected void addChange(OrderStagedChange.AddLineItemAction action) {
        if (action instanceof OrderStagedChange.AddVariant av) this.changes.add(OrderEditMapper.map(av));
        else if (action instanceof OrderStagedChange.AddCustomItem aci) this.changes.add(OrderEditMapper.map(aci));
        else throw new IllegalArgumentException("unknown implementation type of AddLineItemAction");
    }

    protected void addChange(OrderStagedChange.AddItemDiscount addItemDiscount) {
        this.changes.add(OrderEditMapper.map(addItemDiscount));
    }

    protected void addChange(OrderStagedChange.QuantityAdjustmentAction action) {
        if (action instanceof OrderStagedChange.IncrementItem ii) this.changes.add(OrderEditMapper.map(ii));
        else if (action instanceof OrderStagedChange.DecrementItem di) this.changes.add(OrderEditMapper.map(di));
        else throw new IllegalArgumentException();
    }
}
