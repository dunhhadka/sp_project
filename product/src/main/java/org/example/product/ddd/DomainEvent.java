package org.example.product.ddd;

import java.time.Instant;

public interface DomainEvent {
    Instant happenedAt();

    String eventName();
}
