package org.example.product.product.domain.product.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.DomainEvent;

import java.time.Instant;

@Getter
@NoArgsConstructor
public abstract class DomainEventBase implements DomainEvent {
    private final Instant happenedAt = Instant.now();

    @Override
    public Instant happenedAt() {
        return this.happenedAt;
    }
}
