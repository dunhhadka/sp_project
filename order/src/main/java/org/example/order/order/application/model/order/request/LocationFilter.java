package org.example.order.order.application.model.order.request;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder(toBuilder = true)
public class LocationFilter {
    private int limit;
    private int page;
    private Long id;
    private List<Long> locationIds;
    private Boolean defaultLocation;
    private boolean inventoryManagement;
}
