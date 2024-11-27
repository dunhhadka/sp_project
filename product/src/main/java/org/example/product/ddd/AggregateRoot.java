package org.example.product.ddd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MappedSuperclass
public abstract class AggregateRoot<R extends AggregateRoot<R>> extends DomainEntity<R> {

    @Transient
    private List<DomainEvent> events = new ArrayList<>();

    @JsonIgnore
    public List<DomainEvent> getEvents() {
        if (this.events == null) {
            return Collections.emptyList();
        }
        return this.events;
    }

    @PostPersist
    @PostRemove
    void clearEvents() {
        if (this.events != null) {
            this.events.clear();
        }
    }

    protected void addDomainEvents(DomainEvent event) {
        if (this.events == null) events = new ArrayList<>();
        this.events.add(event);
    }
}
