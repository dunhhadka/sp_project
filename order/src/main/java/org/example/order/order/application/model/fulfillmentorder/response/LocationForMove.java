package org.example.order.order.application.model.fulfillmentorder.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocationForMove {
    private Location location;
    private String message;
    private boolean movable;

    @Getter
    @Builder
    public static class Location {
        private int id;
        private String name;
    }
}
