package org.example.order.order.application.service.orderedit;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderEditRequest {

    @Getter
    @Setter
    public static class AddVariant {
        private @Positive int variantId;

        private @Positive int quantity;

        private @Min(0) Integer locationId;

        private boolean allowDuplicate = true;
    }

    @Getter
    @Setter
    public static class AddVariants {
        private List<@Valid AddVariant> variants;
    }

    @Getter
    @Setter
    public static class AddCustomItem {
        private @NotBlank @Size(max = 2000) String title;

        private @NotNull @Min(0) BigDecimal price;

        private @Positive int quantity;

        private @Min(0) Long locationId;

        private boolean requireShipping;

        private boolean taxable = true;
    }

    @Getter
    @Setter
    public static class SetItemQuantity {
        private @NotBlank @Size(max = 36) String lineItemId;

        private @Min(0) int quantity;

        private boolean restock;
    }

    @Getter
    @Setter
    public static class SetItemDiscount {
        private @NotBlank @Size(max = 36) String lineItemId;
        private @Size(max = 255) String description;
        private @Min(0) BigDecimal fixedValue;
        private @Min(0) @Max(100) BigDecimal percentValue;
    }
}
