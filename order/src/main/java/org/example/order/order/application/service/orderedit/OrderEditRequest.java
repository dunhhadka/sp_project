package org.example.order.order.application.service.orderedit;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OrderEditRequest {

    @Getter
    @Setter
    public static class AddVariants {
        private @NotEmpty List<@Valid AddVariant> addVariants;
    }

    @Getter
    @Setter
    public static class AddVariant {
        private @Min(0) int variantId;
        private @Min(0) BigDecimal quantity;
        private Integer locationId;
        private boolean allowDuplicate;
    }
}
