package org.example.order.order.application.service.orderedit;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OrderEditRequest {

    @Getter
    @Setter
    @Builder
    public static class AddVariants {
        private @NotEmpty List<@Valid AddVariant> addVariants;
    }

    @Getter
    @Setter
    @Builder
    public static class AddVariant {
        private @Min(0) int variantId;
        private @Min(0) BigDecimal quantity;
        private Integer locationId;
        private boolean allowDuplicate;
    }

    @Getter
    @Setter
    public static class Increment {
        private @NotNull String lineItemId;
        private @Positive int delta;
        private @Positive Integer locationId;
    }

    @Getter
    @Setter
    public static class Decrement {
        private @NotNull String lineItemId;
        private @NotNull int delta;
        private @Positive Integer locationId;
    }

    @Getter
    @Setter
    public static class SetItemQuantity {
        private @NotBlank @Size(max = 35) String lineItemId;
        private @Min(0) int quantity;
        private @Min(0) Integer locationId;
        private boolean restock;
    }
}
