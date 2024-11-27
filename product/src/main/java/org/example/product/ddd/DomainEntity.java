package org.example.product.ddd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

@MappedSuperclass
public abstract class DomainEntity<R extends AggregateRoot<R>> {

    @Transient
    private boolean isNew = true;

    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    @JsonIgnore
    public void markNotNew() {
        this.isNew = false;
    }
}
