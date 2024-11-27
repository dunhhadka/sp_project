package org.example.order.order.application.model.draftorder.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Getter
@Setter
public class DraftShippingLineRequest {
    private boolean custom;
    private @Size(max = 100) String alias;
    private @Size(max = 150) String title;
    private @Min(0) BigDecimal price;
    private @Size(max = 50) String source;
    private @Size(max = 50) String code;
}
