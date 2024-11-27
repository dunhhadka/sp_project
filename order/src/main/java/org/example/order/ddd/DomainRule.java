package org.example.order.ddd;

public interface DomainRule {
    boolean isBroken();

    String message();
}
