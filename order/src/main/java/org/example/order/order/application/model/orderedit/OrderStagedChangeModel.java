package org.example.order.order.application.model.orderedit;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class OrderStagedChangeModel {

    private final OrderStagedChange.ChangeType changeType;

    public OrderStagedChangeModel(OrderStagedChange.ChangeType changeType) {
        this.changeType = changeType;
    }

    @Getter
    @Setter
    public static class IncrementItem extends OrderStagedChangeModel {
        public IncrementItem() {
            super(OrderStagedChange.ChangeType.increment_item);
        }

        private int delta;
        private int lineItemId;
        private Integer locationId;
    }

    @Getter
    @Setter
    public static class DecrementItem extends OrderStagedChangeModel {
        public DecrementItem() {
            super(OrderStagedChange.ChangeType.decrement_item);
        }

        private int lineItemId;
        private int delta;
        private Integer locationId;
    }

    @Getter
    @Setter
    public static class AddedVariant extends OrderStagedChangeModel {
        public AddedVariant() {
            super(OrderStagedChange.ChangeType.add_variant);
        }

        private UUID lineItemId;
        private int variantId;
        private BigDecimal quantity;
        private Integer locationId;
    }

    @Getter
    @Setter
    public static class AddCustomItem extends OrderStagedChangeModel {
        public AddCustomItem() {
            super(OrderStagedChange.ChangeType.add_custom_item);
        }

        private UUID lineItemId;

        private String title;

        private BigDecimal price;
        private BigDecimal quantity;

        private boolean requireShipping;
        private boolean taxable;

        private Integer locationId;
    }

    @Getter
    @Setter
    public static class AddItemDiscount extends OrderStagedChangeModel {
        public AddItemDiscount() {
            super(OrderStagedChange.ChangeType.add_item_discount);
        }

        private UUID lineItemId;
        private UUID applicationId;

        private String description;
        private BigDecimal value;
    }

}
