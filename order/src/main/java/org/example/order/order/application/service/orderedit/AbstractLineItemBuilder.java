package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.CalculatedLineItem;
import org.example.order.order.application.model.orderedit.OrderStagedChangeModel;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.OrderEditLineItemDto;

import java.util.ArrayList;
import java.util.List;

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
}
