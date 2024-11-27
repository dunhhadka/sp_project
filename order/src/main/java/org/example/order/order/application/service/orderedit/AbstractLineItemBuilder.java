package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.model.orderedit.CalculatedLineItem;
import org.example.order.order.application.model.orderedit.OrderStagedChangeModel;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.LineItemDto;
import org.example.order.order.infrastructure.data.dto.OrderEditLineItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

abstract class AbstractLineItemBuilder<T> implements BuilderSteps.Builder, BuilderSteps.BuilderResult {

    private final CalculatedLineItem calculatedLineItem;

    protected final T context;

    protected final List<OrderStagedChangeModel> changes;

    protected AbstractLineItemBuilder(OrderEditLineItemDto lineItem, T context) {
        this(new CalculatedLineItem(lineItem), context);
    }

    private AbstractLineItemBuilder(CalculatedLineItem calculatedLineItem, T context) {
        this.calculatedLineItem = calculatedLineItem;
        this.context = context;
        this.changes = new ArrayList<>();
    }

    public AbstractLineItemBuilder(LineItemDto lineItem, T context) {
        this(new CalculatedLineItem(lineItem), context);
    }

    /**
     * sau khi thực hiện các bước build chung, sau đó sẽ build những phần khác biệt với nhau
     */
    @Override
    public BuilderSteps.BuilderResult build() {
        doBuild();
        this.calculatedLineItem.setStagedChanges(changes);
        return this;
    }

    protected abstract void doBuild();

    protected void addChange(OrderStagedChange.IncrementItem incrementItem) {
        addChange(StagedChangeModelMapper.toModel(incrementItem));
    }

    protected void addChange(OrderStagedChange.DecrementItem decrementItem) {
        addChange(StagedChangeModelMapper.toModel(decrementItem));
    }

    protected void addChange(OrderStagedChange.AddLineItemAction addAction) {
        if (addAction instanceof OrderStagedChange.AddVariant aa) addChange(StagedChangeModelMapper.toModel(aa));
        else if (addAction instanceof OrderStagedChange.AddCustomItem aci)
            addChange(StagedChangeModelMapper.toModel(aci));
        else throwUnsupportedAction();
    }

    protected void addChange(OrderStagedChange.AddItemDiscount addItemDiscount) {
        addChange(StagedChangeModelMapper.toModel(addItemDiscount));
    }

    private void throwUnsupportedAction() {
        throw new ConstrainViolationException("order_edit_action", "un_supported_for_action");
    }

    private void addChange(OrderStagedChangeModel model) {
        this.changes.add(model); // changes always not null
    }

    @Override
    public CalculatedLineItem lineItem() {
        return calculatedLineItem;
    }

    @Override
    public Map<MergedTaxLine.TaxLineKey, MergedTaxLine> taxLines() {
        return streamTaxLines().collect(MergedTaxLine.toMergedMap());
    }

    abstract Stream<? extends GenerateTaxLine> streamTaxLines();

}
