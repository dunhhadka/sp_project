package org.example.order.order.application.model.order.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ShippingLineResponse {
    private int id;
    private String title;
    private String code;
    private String source;
    private BigDecimal price;
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private List<DiscountAllocationResponse> discountAllocations;
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private List<TaxLineResponse> taxLines;
}
