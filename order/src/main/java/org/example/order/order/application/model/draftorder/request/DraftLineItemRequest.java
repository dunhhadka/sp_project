package org.example.order.order.application.model.draftorder.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DraftLineItemRequest {
    private Integer variantId;
    private Integer productId;
    private @Size(max = 250) String title;
    private @Size(max = 250) String variantTitle;
    private @Min(0) BigDecimal price;
    private @Min(1) int quantity;
    private @Min(0) int grams;
    private Boolean requireShipping;
    private boolean taxable;
    private @Size(max = 100) String sku;
    private @Size(max = 100) String vendor;
    private @Size(max = 100) String fulfillmentService;

    private List<@Valid DraftPropertyRequest> properties;

    private @Valid DraftAppliedDiscountRequest appliedDiscount;

    private String token;

    public boolean isCustom() {
        return this.variantId == null;
    }
}
