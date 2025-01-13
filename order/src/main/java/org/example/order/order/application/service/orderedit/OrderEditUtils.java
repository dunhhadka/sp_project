package org.example.order.order.application.service.orderedit;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.domain.order.model.BillingAddress;
import org.example.order.order.domain.order.model.MailingAddress;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.ShippingAddress;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.OrderStagedChangeDto;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderEditUtils {

    public static GroupedStagedChange groupStagedChange(List<OrderStagedChangeDto> changes) {
        List<OrderStagedChange> changeModels = toModelChanges(changes);

        List<OrderStagedChange.AddVariant> addVariants = new ArrayList<>();
        List<OrderStagedChange.AddCustomItem> addCustomItems = new ArrayList<>();
        List<OrderStagedChange.IncrementItem> incrementItems = new ArrayList<>();
        List<OrderStagedChange.DecrementItem> decrementItems = new ArrayList<>();
        List<OrderStagedChange.AddItemDiscount> addItemDiscounts = new ArrayList<>();

        changeModels
                .forEach(switchActionChange(
                        addVariants::add,
                        addCustomItems::add,
                        incrementItems::add,
                        decrementItems::add,
                        addItemDiscounts::add
                ));

        return new GroupedStagedChange(
                addVariants,
                addCustomItems,
                incrementItems,
                decrementItems,
                addItemDiscounts
        );
    }

    private static Consumer<OrderStagedChange> switchActionChange(
            Consumer<OrderStagedChange.AddVariant> addVariantConsumer,
            Consumer<OrderStagedChange.AddCustomItem> addCustomItemConsumer,
            Consumer<OrderStagedChange.IncrementItem> incrementItemConsumer,
            Consumer<OrderStagedChange.DecrementItem> decrementItemConsumer,
            Consumer<OrderStagedChange.AddItemDiscount> addItemDiscountConsumer
    ) {
        return change -> {
            OrderStagedChange.BaseAction action = change.getAction();
            if (action instanceof OrderStagedChange.AddVariant av) addVariantConsumer.accept(av);
            else if (action instanceof OrderStagedChange.AddCustomItem aci) addCustomItemConsumer.accept(aci);
            else if (action instanceof OrderStagedChange.IncrementItem ii) incrementItemConsumer.accept(ii);
            else if (action instanceof OrderStagedChange.DecrementItem di) decrementItemConsumer.accept(di);
            else if (action instanceof OrderStagedChange.AddItemDiscount aid) addItemDiscountConsumer.accept(aid);
            else {
                throw new IllegalArgumentException("unsupported for change");
            }
        };
    }

    private static List<OrderStagedChange> toModelChanges(List<OrderStagedChangeDto> changes) {
        if (CollectionUtils.isEmpty(changes))
            return Collections.emptyList();

        return changes.stream()
                .map(OrderEditUtils::convert)
                .toList();
    }

    private static OrderStagedChange convert(OrderStagedChangeDto change) {
        try {
            OrderStagedChange.BaseAction action = JsonUtils.unmarshal(change.getValue(), OrderStagedChange.BaseAction.class);
            return new OrderStagedChange(change.getId(), change.getType(), action);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String resolveOrderCountryCode(Order order) {
        return Optional.ofNullable(order.getBillingAddress())
                .map(BillingAddress::getAddressInfo)
                .or(() -> Optional.ofNullable(order.getShippingAddress())
                        .map(ShippingAddress::getAddressInfo))
                .map(MailingAddress::getCountryCode)
                .orElse("VND");
    }

    public static Pair<UUID, Integer> parseLineItemId(String lineItemIdString) {
        try {
            if (StringUtils.isNotBlank(lineItemIdString)) {
                if (lineItemIdString.contains("-")) {
                    UUID lineItemId = UUID.fromString(lineItemIdString);
                    return Pair.of(lineItemId, null);
                } else {
                    int lineItemId = Integer.parseInt(lineItemIdString);
                    return Pair.of(null, lineItemId);
                }
            }
        } catch (Exception ignored) {
            log.error("invalid line item id");
        }
        throw new ConstrainViolationException(UserError.builder()
                .message("line item id is invalid")
                .build());
    }

    public record GroupedStagedChange(
            List<OrderStagedChange.AddVariant> addVariants,
            List<OrderStagedChange.AddCustomItem> addCustomItems,
            List<OrderStagedChange.IncrementItem> incrementItems,
            List<OrderStagedChange.DecrementItem> decrementItems,
            List<OrderStagedChange.AddItemDiscount> addItemDiscounts
    ) {
        Stream<OrderStagedChange.QuantityAdjustmentAction> quantityAdjustmentStream() {
            return Stream.concat(this.incrementItems.stream(), this.decrementItems.stream());
        }

        Stream<OrderStagedChange.AddLineItemAction> addActionsStream() {
            return Stream.concat(this.addVariants.stream(), this.addCustomItems.stream());
        }
    }
}
