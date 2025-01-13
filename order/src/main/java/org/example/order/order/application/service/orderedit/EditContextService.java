package org.example.order.order.application.service.orderedit;

import com.google.common.base.Verify;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.SapoClient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.application.utils.*;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.orderedit.model.AddedLineItem;
import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.domain.orderedit.persistence.OrderEditRepository;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dto.Location;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EditContextService {

    private final SapoClient sapoClient;

    private final TaxHelper taxHelper;

    private final OrderRepository orderRepository;
    private final OrderEditRepository orderEditRepository;
    private final ProductDao productDao;

    public AddVariantsContext createContext(OrderEditId orderEditId, List<OrderEditRequest.AddVariant> addVariants) {
        return new AddVariantsContextImpl(orderEditId, addVariants);
    }

    public AddCustomItemContext createContext(OrderEditId orderEditId, OrderEditRequest.AddCustomItem request) {
        return new AddCustomItemContextImpl(orderEditId, request);
    }

    public SetQuantityContext createContext(OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        Pair<UUID, Integer> lineItemIdPair = OrderEditUtils.parseLineItemId(request.getLineItemId());
        if (lineItemIdPair.getKey() != null) {
            return createAddedLineContext(lineItemIdPair.getKey(), orderEditId, request);
        }
        return createOrderLineItemContext(lineItemIdPair.getValue(), orderEditId, request);
    }

    private SetQuantityContext createOrderLineItemContext(Integer lineItemId, OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        OrderEdit orderEdit = fetchOrderEdit(orderEditId);
        Order order = getOrder(orderEditId.getStoreId(), orderEdit.getOrderId());
        LineItem lineItem = order.getLineItems().stream()
                .filter(line -> line.getId() == lineItemId)
                .findFirst()
                .orElseThrow(() -> new ConstrainViolationException(UserError.builder()
                        .message("line item not found")
                        .build()));
        int editableQuantity = lineItem.getFulfillableQuantity();
        int requestedQuantity = request.getQuantity();
        if (requestedQuantity > editableQuantity) {
            return new IncreaseItemContext(orderEdit, request, lineItem);
        } else if (requestedQuantity < editableQuantity) {
            return new DecreaseItemContext(orderEdit, request, lineItem);
        }
        return new ResetItemContext(orderEdit, request, lineItem);
    }

    public interface ApplyDiscount extends OrderEditContext<OrderEditRequest.SetItemDiscount>, NeededTax {
        OrderEdit.DiscountRequest discountRequest();

        AddedLineItem lineItem();
    }

    public ApplyDiscount createContext(OrderEditId orderEditId, OrderEditRequest.SetItemDiscount request) {
        return new ApplyDiscountContext(orderEditId, request);
    }

    private final class ApplyDiscountContext extends AbstractContext<OrderEditRequest.SetItemDiscount>
            implements ApplyDiscount {

        private final AddedLineItem lineItem;
        private final OrderEdit.DiscountRequest discountRequest;
        private final TaxSetting taxSetting;

        private ApplyDiscountContext(OrderEditId orderEditId, OrderEditRequest.SetItemDiscount request) {
            super(orderEditId, request);

            this.lineItem = orderEdit().getLineItems().stream()
                    .filter(line -> line.getId().equals(OrderEditUtils.parseLineItemId(request.getLineItemId()).getKey()))
                    .findFirst()
                    .orElseThrow();

            this.discountRequest = resolveDiscountRequest(request());

            if (!lineItem.isTaxable()) {
                this.taxSetting = null;
                return;
            }

            Integer productId = lineItem.getProductId();
            this.taxSetting = fetchTaxes(Set.of(productId));
        }

        private OrderEdit.DiscountRequest resolveDiscountRequest(OrderEditRequest.SetItemDiscount request) {
            if (request.getFixedValue() != null) {
                return new OrderEdit.DiscountRequest(
                        DiscountApplication.ValueType.fixed_amount,
                        request.getFixedValue(),
                        request.getDescription()
                );
            }
            if (request.getPercentValue() != null) {
                var value = request.getPercentValue()
                        .setScale(2, RoundingMode.CEILING)
                        .min(BigDecimals.ONE_HUNDRED);
                return new OrderEdit.DiscountRequest(
                        DiscountApplication.ValueType.percentage, value,
                        request.getDescription());
            }

            throw new IllegalArgumentException();
        }

        @Override
        public OrderEdit.DiscountRequest discountRequest() {
            return this.discountRequest;
        }

        @Override
        public AddedLineItem lineItem() {
            return this.lineItem;
        }

        @Override
        public TaxSetting taxSetting() {
            return this.taxSetting;
        }
    }

    private interface ForExistingItem {
        LineItem lineItem();
    }

    public abstract class ExistingItemContext extends AbstractContext<OrderEditRequest.SetItemQuantity>
            implements SetQuantityContext, ForExistingItem {

        private final LineItem lineItem;

        protected ExistingItemContext(OrderEdit orderEdit, OrderEditRequest.SetItemQuantity request, LineItem lineItem) {
            super(orderEdit, request);
            this.lineItem = lineItem;
        }

        @Override
        public LineItem lineItem() {
            return this.lineItem;
        }
    }

    private interface ForIncreaseItem {
        TaxSetting taxSetting();
    }

    private final class IncreaseItemContext extends ExistingItemContext implements ForIncreaseItem {
        private final TaxSetting taxSetting;

        private IncreaseItemContext(OrderEdit orderEdit, OrderEditRequest.SetItemQuantity request, LineItem lineItem) {
            super(orderEdit, request, lineItem);
            if (!lineItem.isTaxable()) {
                this.taxSetting = null;
                return;
            }

            Integer productId = lineItem.getVariantInfo().getProductId();
            this.taxSetting = fetchTaxes(Set.of(productId));
        }

        @Override
        public TaxSetting taxSetting() {
            return this.taxSetting;
        }
    }

    private final class DecreaseItemContext extends ExistingItemContext {

        private DecreaseItemContext(OrderEdit orderEdit, OrderEditRequest.SetItemQuantity request, LineItem lineItem) {
            super(orderEdit, request, lineItem);
        }
    }

    private final class ResetItemContext extends ExistingItemContext {

        private ResetItemContext(OrderEdit orderEdit, OrderEditRequest.SetItemQuantity request, LineItem lineItem) {
            super(orderEdit, request, lineItem);
        }
    }

    private SetQuantityContext createAddedLineContext(UUID addedLineItemId, OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        OrderEdit orderEdit = fetchOrderEdit(orderEditId);
        AddedLineItem lineItem = orderEdit.getLineItems().stream()
                .filter(line -> Objects.equals(line.getId(), addedLineItemId))
                .findFirst()
                .orElseThrow(() -> new ConstrainViolationException(UserError.builder()
                        .message("line item not found")
                        .build()));
        if (request.getQuantity() == 0) {
            return new RemovedItemContext(orderEdit, request, lineItem);
        }
        return new AdjustAddedItemContext(orderEdit, request, lineItem);
    }

    interface ForAddedItemContext {
        AddedLineItem lineItem();
    }

    private abstract class AddedLineItemContext extends AbstractContext<OrderEditRequest.SetItemQuantity>
            implements ForAddedItemContext, SetQuantityContext, NeededTax {
        private final AddedLineItem lineItem;
        private final TaxSetting taxSetting;

        protected AddedLineItemContext(OrderEdit orderEdit, OrderEditRequest.SetItemQuantity request, AddedLineItem lineItem) {
            super(orderEdit, request);
            this.lineItem = lineItem;

            Integer productId = lineItem.getProductId();
            this.taxSetting = fetchTaxes(Set.of(productId));
        }

        @Override
        public AddedLineItem lineItem() {
            return lineItem;
        }

        @Override
        public TaxSetting taxSetting() {
            return this.taxSetting;
        }
    }

    public final class RemovedItemContext extends AddedLineItemContext {

        private RemovedItemContext(OrderEdit orderEdit, OrderEditRequest.SetItemQuantity request, AddedLineItem lineItem) {
            super(orderEdit, request, lineItem);
        }
    }

    public interface NeededTax {
        TaxSetting taxSetting();
    }

    private interface AdjustAddedItem {
    }

    public final class AdjustAddedItemContext extends AddedLineItemContext
            implements AdjustAddedItem {

        private AdjustAddedItemContext(OrderEdit orderEdit, OrderEditRequest.SetItemQuantity request, AddedLineItem lineItem) {
            super(orderEdit, request, lineItem);
        }
    }

    public interface SetQuantityContext extends OrderEditContext<OrderEditRequest.SetItemQuantity> {

    }

    public interface AddCustomItemContext extends OrderEditContext<OrderEditRequest.AddCustomItem>, NeedTax {
        TaxSetting taxSetting();

        Location location();
    }

    private final class AddCustomItemContextImpl extends AbstractContext<OrderEditRequest.AddCustomItem>
            implements AddCustomItemContext {

        private final TaxSetting taxSetting;
        private final Location location;

        public AddCustomItemContextImpl(OrderEditId orderEditId, OrderEditRequest.AddCustomItem request) {
            super(orderEditId, request);

            this.taxSetting = fetchTaxes(Set.of(0));
            this.location = doFetchLocation();
        }

        private Location doFetchLocation() {
            Long locationId = this.request().getLocationId();
            var locationMap = fetchLocations(
                    locationId == null,
                    Optional.ofNullable(locationId).map(List::of).orElse(List.of())
            );
            return locationMap.get(locationId);
        }

        @Override
        public TaxSetting taxSetting() {
            return this.taxSetting;
        }

        @Override
        public Location location() {
            return this.location;
        }

        @Override
        public TaxSettingValue getTax(Integer productId) {
            return null;
        }

        @Override
        public boolean taxIncluded() {
            return false;
        }

        @Override
        public boolean isTaxExempt() {
            return false;
        }
    }

    private interface OrderEditContext<T> {
        T request();

        OrderEdit orderEdit();

        Order order();
    }

    public interface NeedTax {
        TaxSettingValue getTax(Integer productId);

        boolean taxIncluded();

        boolean isTaxExempt();
    }

    public interface AddVariantsContext extends OrderEditContext<List<OrderEditRequest.AddVariant>>, NeedTax {
        record VariantInfo(ProductDto product, VariantDto variant) {
        }

        VariantInfo getVariantInfo(int variantId);

        Location getLocation(Long id);
    }

    private Order getOrder(int storeId, int id) {
        var orderId = new OrderId(storeId, id);
        var order = orderRepository.findById(orderId);
        if (order == null) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not found")
                    .build());
        }
        return order;
    }

    public OrderEdit fetchOrderEdit(OrderEditId orderEditId) {
        var orderEdit = orderEditRepository.findById(orderEditId);
        if (orderEditId == null)
            throw new NotFoundException("Order edit not found");

        if (orderEdit.getCommittedAt() != null) {
            throw new ConstrainViolationException(UserError.builder()
                    .fields(List.of("order_edit_id"))
                    .code("invalid")
                    .message("committed order cannot be edited")
                    .build());
        }
        return orderEdit;
    }

    private abstract class AbstractContext<T> implements OrderEditContext<T> {
        private final T request;
        private final OrderEdit orderEdit;
        private @Nullable Order order;

        protected AbstractContext(OrderEditId orderEditId, T request) {
            this.request = request;
            this.orderEdit = fetchOrderEdit(orderEditId);
        }

        protected AbstractContext(OrderEdit orderEdit, T request) {
            this.request = request;
            this.orderEdit = orderEdit;
        }

        @Override
        public T request() {
            return this.request;
        }

        @Override
        public OrderEdit orderEdit() {
            return this.orderEdit;
        }

        @Override
        public Order order() {
            if (order == null) order = getOrder(storeId(), orderEdit.getOrderId());
            return order;
        }

        private int storeId() {
            return this.orderEdit.getId().getStoreId();
        }

        public record FetchedVariants(Map<Integer, ProductDto> products, Map<Integer, VariantDto> variants) {
        }

        final FetchedVariants fetchVariants(List<Integer> variantIds) {
            if (CollectionUtils.isNotEmpty(variantIds))
                return new FetchedVariants(Map.of(), Map.of());

            var variantMap = productDao.findVariantByListIds(storeId(), variantIds).stream()
                    .collect(Collectors.toMap(VariantDto::getId, Function.identity()));

            String variantNotExist = variantIds.stream()
                    .filter(id -> !variantMap.containsKey(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            if (StringUtils.isNotBlank(variantNotExist)) {
                throw new ConstrainViolationException(UserError.builder()
                        .code("not_existed")
                        .message("variants not existed: " + variantNotExist)
                        .fields(List.of("variant_id"))
                        .build());
            }

            var productIds = variantMap.values().stream()
                    .map(VariantDto::getProductId)
                    .filter(NumberUtils::isPositive)
                    .distinct()
                    .toList();
            var productMap = productDao.findProductByListIds(storeId(), productIds)
                    .stream().collect(Collectors.toMap(ProductDto::getId, Function.identity()));

            return new FetchedVariants(productMap, variantMap);
        }

        final Map<Long, Location> fetchLocations(boolean existedDefaultLocation, List<Long> locationIds) {
            var filter = LocationFilter.builder()
                    .locationIds(
                            locationIds.stream()
                                    .filter(NumberUtils::isPositive)
                                    .toList()
                    );
            if (existedDefaultLocation) filter.defaultLocation(true);

            var locationMap = sapoClient.locationList(filter.build())
                    .stream().collect(Collectors.toMap(Location::getId, Function.identity()));

            // validate
            String notExisted = locationIds.stream()
                    .filter(id -> !locationMap.containsKey(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            if (StringUtils.isNotBlank(notExisted)) {
                throw new ConstrainViolationException(UserError.builder()
                        .message(notExisted)
                        .code("not existed")
                        .build());
            }

            if (existedDefaultLocation) {
                var defaultLocation = locationMap.values().stream()
                        .filter(Location::isDefaultLocation)
                        .findFirst()
                        .orElse(null);
                if (defaultLocation == null) {
                    throw new ConstrainViolationException(UserError.builder()
                            .message("Default location not found")
                            .build());
                }
                locationMap.put(null, defaultLocation);
            }

            return locationMap;
        }

        final TaxSetting fetchTaxes(Set<Integer> productIds) {
            Verify.verify(orderEdit != null, "Expect orderEdit to have value");

            if (order == null) order = getOrder(storeId(), orderEdit.getOrderId());

            if (CollectionUtils.isEmpty(productIds))
                return TaxSetting.defaultTax();

            if (order.isTaxExempt())
                return TaxSetting.defaultTax();

            String countryCode = OrderEditUtils.resolveOrderCountryCode(order);
            if (!"VND".equals(countryCode))
                return TaxSetting.defaultTax();

            var taxSetting = taxHelper.getTaxSetting(storeId(), countryCode, productIds);
            taxSetting = taxSetting.toBuilder().taxIncluded(order.isTaxIncluded()).build();

            return taxSetting;
        }
    }

    /**
     * context chứa thông tin khi add 1 danh sách variants
     * => context bao gồm List<Product>, List<Variants>, List<Tax>, List<Location> => đưa về map
     */
    private final class AddVariantsContextImpl extends AbstractContext<List<OrderEditRequest.AddVariant>>
            implements AddVariantsContext {

        private final Map<Integer, ProductDto> products;
        private final Map<Integer, VariantDto> variants;
        private final Map<Long, Location> locations;
        private final TaxSetting taxSetting;

        public AddVariantsContextImpl(OrderEditId orderEditId, List<OrderEditRequest.AddVariant> addVariants) {
            super(orderEditId, addVariants);

            var variantIds = addVariants.stream()
                    .map(OrderEditRequest.AddVariant::getVariantId)
                    .filter(NumberUtils::isPositive)
                    .distinct()
                    .toList();
            FetchedVariants fetchedVariants = fetchVariants(variantIds);
            this.products = fetchedVariants.products;
            this.variants = fetchedVariants.variants;

            var locationIds = addVariants.stream()
                    .map(OrderEditRequest.AddVariant::getLocationId)
                    .map(Long::valueOf)
                    .filter(NumberUtils::isPositive)
                    .distinct()
                    .toList();
            boolean existedDefaultLocation = addVariants.stream().anyMatch(add -> add.getLocationId() == null);
            locations = fetchLocations(existedDefaultLocation, locationIds);

            Set<Integer> productIds = products.keySet();
            productIds.add(0);
            this.taxSetting = fetchTaxes(productIds);
        }

        @Override
        public VariantInfo getVariantInfo(int variantId) {
            var variant = variants.get(variantId);
            assert variant != null : "Require valid variantId";

            var product = products.get(variant.getProductId());
            assert product != null;

            return new VariantInfo(product, variant);
        }

        @Override
        public Location getLocation(Long id) {
            return locations.get(id);
        }

        @Override
        public TaxSettingValue getTax(Integer productId) {
            return taxSetting.getTaxes().stream()
                    .filter(tax -> Objects.equals(tax.getProductId(), productId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean taxIncluded() {
            return taxSetting.isTaxIncluded();
        }

        @Override
        public boolean isTaxExempt() {
            return order().isTaxExempt();
        }
    }
}
