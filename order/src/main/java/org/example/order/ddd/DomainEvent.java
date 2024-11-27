package org.example.order.ddd;

import java.time.Instant;

public interface DomainEvent {
    Instant happenedAt();

    String eventName();
}
