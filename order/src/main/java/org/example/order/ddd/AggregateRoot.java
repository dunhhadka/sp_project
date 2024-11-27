package org.example.order.ddd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MappedSuperclass
public abstract class AggregateRoot<R extends AggregateRoot<R>> extends DomainEntity<R> {
    @Transient
    protected List<DomainEvent> events;

    @JsonIgnore
    public List<DomainEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    protected void addDomainEvent(DomainEvent event) {
        if (this.events == null) events = new ArrayList<>();
        events.add(event);
    }
}
