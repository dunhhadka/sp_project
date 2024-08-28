package org.example.order.ddd;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AggregateRoot<R extends AggregateRoot<R>> extends DomainEntity<R> {

}
