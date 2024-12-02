package org.example.order.order.domain.orderedit.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import lombok.*;
import org.example.order.order.application.utils.JsonUtils;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "order_staged_changes")
public class OrderStagedChange {
    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "editing_id", referencedColumnName = "id")
    @JoinColumn(name = "store_id", referencedColumnName = "store_id")
    private OrderEdit aggRoot;

    @Id
    private UUID id;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private ChangeType type;

    @Column(name = "action")
    @Convert(converter = BaseAction.Converter.class)
    private BaseAction action;

    public OrderStagedChange(UUID id, ChangeType type, BaseAction action) {
        this.id = id;
        this.type = type;
        this.action = action;
    }

    public void updateEvent(BaseAction newEvent) {
        this.action = newEvent;
    }

    public void update(ChangeType type, BaseAction action) {
        this.type = type;
        this.action = action;
    }

    public interface QuantityAdjustmentAction {
        int getLineItemId();

        int getDelta();
    }

    public interface AddLineItemAction {
        UUID getLineItemId();

        BigDecimal getQuantity();

        Integer getLocationId();
    }

    @Getter
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = BaseAction.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = AddVariant.class, name = "add_variant"),
            @JsonSubTypes.Type(value = AddCustomItem.class, name = "add_custom_item"),
            @JsonSubTypes.Type(value = IncrementItem.class, name = "increment_item"),
            @JsonSubTypes.Type(value = DecrementItem.class, name = "decrement_item"),
            @JsonSubTypes.Type(value = AddItemDiscount.class, name = "add_item_discount")
    })
    public static class BaseAction {
        private final ChangeType type;

        public BaseAction(ChangeType type) {
            this.type = type;
        }

        @jakarta.persistence.Converter
        public static class Converter implements AttributeConverter<BaseAction, String> {

            @Override
            @SneakyThrows
            public String convertToDatabaseColumn(BaseAction baseAction) {
                return JsonUtils.marshal(baseAction);
            }

            @Override
            @SneakyThrows
            public BaseAction convertToEntityAttribute(String data) {
                return JsonUtils.unmarshal(data, BaseAction.class);
            }
        }
    }

    @Getter
    @Setter
    @Builder(toBuilder = true)
    public static class AddVariant extends BaseAction implements AddLineItemAction {
        @NotNull
        private Integer variantId;
        @NotNull
        private UUID lineItemId;
        @NotNull
        @Min(0)
        private BigDecimal quantity;
        private Integer locationId;

        public AddVariant() {
            super(ChangeType.add_variant);
        }

        public AddVariant(Integer variantId, UUID lineItemId, BigDecimal quantity, Integer locationId) {
            this();
            this.variantId = variantId;
            this.lineItemId = lineItemId;
            this.quantity = quantity;
            this.locationId = locationId;
        }
    }

    @Getter
    @Setter
    @Builder(toBuilder = true)
    public static class AddCustomItem extends BaseAction implements AddLineItemAction {
        @NotNull
        private UUID lineItemId;
        @NotNull
        private String title;
        @NotNull
        @Min(0)
        private BigDecimal price;
        @NotNull
        @Min(0)
        private BigDecimal quantity;
        private boolean taxable;
        private boolean requireShipping;

        private Integer locationId;

        public AddCustomItem() {
            super(ChangeType.add_custom_item);
        }

        public AddCustomItem(
                UUID lineItemId,
                String title,
                BigDecimal price,
                BigDecimal quantity,
                boolean taxable,
                boolean requireShipping,
                Integer locationId
        ) {
            super(ChangeType.add_custom_item);
            this.lineItemId = lineItemId;
            this.title = title;
            this.price = price;
            this.quantity = quantity;
            this.taxable = taxable;
            this.requireShipping = requireShipping;
            this.locationId = locationId;
        }
    }

    @Getter
    @Setter
    @Builder
    public static class IncrementItem extends BaseAction implements QuantityAdjustmentAction {
        @Positive
        private int lineItemId;
        private int delta;
        private Integer locationId;

        public IncrementItem(int lineItemId, int delta, Integer locationId) {
            this();
            this.lineItemId = lineItemId;
            this.delta = delta;
            this.locationId = locationId;
        }

        public IncrementItem() {
            super(ChangeType.increment_item);
        }

        public void update(int delta) {
            this.delta = delta;
        }
    }

    @Getter
    @Setter
    @Builder
    public static class DecrementItem extends BaseAction implements QuantityAdjustmentAction {
        @Positive
        private int lineItemId;
        private int delta;
        private Integer locationId;
        private boolean restock;

        public DecrementItem(int lineItemId, int delta, Integer locationId, boolean restock) {
            this();
            this.lineItemId = lineItemId;
            this.delta = delta;
            this.locationId = locationId;
            this.restock = restock;
        }

        public DecrementItem() {
            super(ChangeType.decrement_item);
        }

        public void update(int delta) {
            this.delta = delta;
        }
    }

    @Getter
    @Setter
    public static class AddItemDiscount extends BaseAction {
        private @NotNull UUID lineItemId;
        private String description;
        private BigDecimal value;
        private UUID applicationId;
        private UUID allocationId;

        public AddItemDiscount() {
            super(ChangeType.add_item_discount);
        }
    }

    public enum ChangeType {
        @JsonEnumDefaultValue
        no_op,
        add_variant,
        add_custom_item,
        add_item_discount,
        increment_item,
        decrement_item,
        add_shipping_line
    }
}
