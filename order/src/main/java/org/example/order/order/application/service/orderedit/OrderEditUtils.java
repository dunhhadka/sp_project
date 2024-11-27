package org.example.order.order.application.service.orderedit;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.domain.order.model.BillingAddress;
import org.example.order.order.domain.order.model.MailingAddress;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.ShippingAddress;
import org.example.order.order.domain.orderedit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dto.OrderStagedChangeDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderEditUtils {

    public static GroupedStagedChange groupChanges(List<OrderStagedChangeDto> stagedChanges) {
        var orderStagedChanges = stagedChanges.stream()
                .map(OrderEditUtils::convert)
                .toList();

        List<OrderStagedChange.AddVariant> addVariants = new ArrayList<>();
        List<OrderStagedChange.AddCustomItem> addCustomItems = new ArrayList<>();
        List<OrderStagedChange.IncrementItem> incrementItems = new ArrayList<>();
        List<OrderStagedChange.DecrementItem> decrementItems = new ArrayList<>();
        List<OrderStagedChange.AddItemDiscount> addItemDiscounts = new ArrayList<>();
        orderStagedChanges
                .stream()
                .map(OrderStagedChange::getAction)
                .forEach(switchAction(
                        addVariants::add,
                        addCustomItems::add,
                        incrementItems::add,
                        decrementItems::add,
                        addItemDiscounts::add
                ));

        return new GroupedStagedChange(
                addVariants, addCustomItems,
                incrementItems, decrementItems, addItemDiscounts);
    }

    private static Consumer<OrderStagedChange.BaseAction> switchAction(
            Consumer<OrderStagedChange.AddVariant> addVariants,
            Consumer<OrderStagedChange.AddCustomItem> addCustomItems,
            Consumer<OrderStagedChange.IncrementItem> incrementItems,
            Consumer<OrderStagedChange.DecrementItem> decrementItems,
            Consumer<OrderStagedChange.AddItemDiscount> addItemDiscounts
    ) {
        return (change) -> {
            if (change instanceof OrderStagedChange.AddVariant av) addVariants.accept(av);
            else if (change instanceof OrderStagedChange.AddCustomItem aci) addCustomItems.accept(aci);
            else if (change instanceof OrderStagedChange.IncrementItem ii) incrementItems.accept(ii);
            else if (change instanceof OrderStagedChange.DecrementItem di) decrementItems.accept(di);
            else if (change instanceof OrderStagedChange.AddItemDiscount aid) addItemDiscounts.accept(aid);
            else {
                throw new IllegalArgumentException("Not supported for change type : " + change.getType());
            }
        };
    }

    public static OrderStagedChange convert(OrderStagedChangeDto change) {
        try {
            OrderStagedChange.BaseAction action = JsonUtils.unmarshal(change.getValue(), OrderStagedChange.BaseAction.class);
            return new OrderStagedChange(change.getId(), change.getType(), action);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCountryCode(Order order) {
        return Optional.ofNullable(order.getBillingAddress())
                .map(BillingAddress::getAddressInfo)
                .or(() ->
                        Optional.ofNullable(order.getShippingAddress()).map(ShippingAddress::getAddressInfo))
                .map(MailingAddress::getCountryCode)
                .orElse("VND");
    }

    public record GroupedStagedChange(
            List<OrderStagedChange.AddVariant> addVariants,
            List<OrderStagedChange.AddCustomItem> addCustomItems,
            List<OrderStagedChange.IncrementItem> incrementItems,
            List<OrderStagedChange.DecrementItem> decrementItems,
            List<OrderStagedChange.AddItemDiscount> addItemDiscounts
    ) {

        public Stream<OrderStagedChange.QuantityAdjustmentAction> getAdjustQuantityChanges() {
            return Stream.concat(incrementItems.stream(), decrementItems.stream());
        }

        public Stream<OrderStagedChange.AddLineItemAction> getAddItemActions() {
            return Stream.concat(addVariants.stream(), addCustomItems.stream());
        }
    }
}
