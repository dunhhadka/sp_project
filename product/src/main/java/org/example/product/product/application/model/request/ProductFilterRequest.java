package org.example.product.product.application.model.request;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ProductFilterRequest extends PagingFilterRequest {
    private String query;
    private String vendor;
    private List<String> tags;
    private Instant createdOnMax;
    private Instant createdOnMin;
}
