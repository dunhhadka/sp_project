package org.example.order.order.application.model.order.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CombinationLineResponse {
    private long id;
    private long variantId;
    private long productId;
    private boolean productExists;
    private String title;
    private String variantTitle;
    private String name;
    private String unit;
    private String itemUnit;
    private String type;
    private String sku;
    private String vendor;
    private BigDecimal price;
    private BigDecimal quantity;
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private ImageResponse image;
    private List<CombinationLineComponentResponse> components;
}
