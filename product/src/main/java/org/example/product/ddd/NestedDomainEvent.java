package org.example.product.ddd;

public abstract class NestedDomainEvent<R extends AggregateRoot<R>> extends DomainEntity<R> {
    protected abstract R getAggRoot();

    protected void addDomainEvents(DomainEvent event) {
        if (getAggRoot() != null) {
            getAggRoot().addDomainEvents(event);
        }
    }
}
