package org.example.order.order.application.model.order.context;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Accessors(fluent = true)
@Jacksonized
public class ProductResponse {
    private List<Product> products;
}
