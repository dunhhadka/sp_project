package org.example.order.order.application.service.orderedit;

import com.google.common.base.Preconditions;
import com.google.common.base.VerifyException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.SapoClient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.TaxHelper;
import org.example.order.order.application.utils.TaxSetting;
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

import javax.validation.constraints.NotEmpty;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderEditContextService {

    private final ProductDao productDao;
    private final OrderEditRepository orderEditRepository;
    private final OrderRepository orderRepository;
    private final SapoClient sapoClient;
    private final TaxHelper taxHelper;

    /**
     * TH1: nếu id là Integer => orderLineItem
     * - nếu quantityRequest > editableQuantity => Increase Line Item Quantity
     * - nếu quantityRequest < editableQuantity => Decrease LineItem Quantity
     * - nếu quantityRequest == editableQuantity => Reset LineItem quantity
     * TH2: nếu id là UUID => addedLineItem
     * - nếu quantityRequest != 0 => adjust LineItem quantity
     * - nếu quantityRequest == 0 => remove LineItem quantity
     */
    public SetQuantityContext createSetItemQuantityContext(OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        return createSetQuantityContext(orderEditId, request);
    }

    public SetQuantityContext createSetQuantityContext(OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        Pair<Integer, UUID> lineItemPair = OrderEditUtils.resolveLineItemId(request.getLineItemId());

        if (lineItemPair.getLeft() != null) {
            return createExistingContext(lineItemPair.getLeft(), orderEditId, request);
        } else if (lineItemPair.getRight() != null) {
            return createAddedLineItemContext(lineItemPair.getRight(), orderEditId, request);
        }

        throw new ConstrainViolationException("line_item_id", "can not resole line_item_id from request");
    }

    private OrderEditAddedLineItem createAddedLineItemContext(UUID addedLineItemId, OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        OrderEdit orderEdit = fetchOrderEdit(orderEditId);
        AddedLineItem lineItem = orderEdit.getLineItems().stream()
                .filter(line -> Objects.equals(line.getId(), addedLineItemId))
                .findFirst()
                .orElseThrow(() -> new ConstrainViolationException(
                        "line_item",
                        "Line item not found with %s".formatted(addedLineItemId)));
        if (request.getQuantity() == 0) {
            return new AddedLineItemRemover(orderEdit, lineItem, request);
        }
        return new ChangeAddedLineItemQuantity(orderEdit, lineItem, request);
    }

    public class ChangeAddedLineItemQuantity extends AbstractContext<OrderEditRequest.SetItemQuantity>
            implements OrderEditAddedLineItem, NeedTax {

        protected final AddedLineItem lineItem;
        protected final TaxSetting taxSetting;

        protected ChangeAddedLineItemQuantity(OrderEdit orderEdit, AddedLineItem lineItem, OrderEditRequest.SetItemQuantity request) {
            super(orderEdit, request);

            this.lineItem = lineItem;
            if (!lineItem.isTaxable()) {
                this.taxSetting = TaxSetting.defaultTax();
                return;
            }

            Set<Integer> productIds = lineItem.getProductId() != null
                    ? new HashSet<>(lineItem.getProductId())
                    : new HashSet<>();
            this.taxSetting = needTaxes(productIds);
        }

        @Override
        public TaxSetting taxSetting() {
            return this.taxSetting;
        }

        @Override
        public AddedLineItem lineItem() {
            return lineItem;
        }
    }

    public class AddedLineItemRemover extends ChangeAddedLineItemQuantity {

        protected AddedLineItemRemover(OrderEdit orderEdit, AddedLineItem lineItem, OrderEditRequest.SetItemQuantity request) {
            super(orderEdit, lineItem, request);
        }
    }

    private ExistingLineItem createExistingContext(Integer lineItemId, OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        OrderEdit orderEdit = fetchOrderEdit(orderEditId);
        Order order = fetchOrder(orderEditId.getStoreId(), orderEdit.getOrderId());
        LineItem lineItem = order.getLineItems().stream()
                .filter(line -> Objects.equals(line.getId(), lineItemId))
                .findFirst()
                .orElseThrow(() -> new ConstrainViolationException(
                        "line_item",
                        "Line item not found with %s".formatted(lineItemId)));
        if (request.getQuantity() > lineItem.getFulfillableQuantity()) {
            return new IncrementLineItemQuantity(orderEdit, order, request, lineItem);
        } else if (request.getQuantity() < lineItem.getFulfillableQuantity()) {
            return new DecrementLineItemQuantity(orderEdit, order, request, lineItem);
        }
        return new ResetLineItemQuantity(orderEdit, order, request, lineItem);
    }

    public class ExistingLineItemImpl extends AbstractContext<OrderEditRequest.SetItemQuantity>
            implements ExistingLineItem {

        protected final LineItem lineItem;
        protected final TaxSetting taxSetting;

        protected ExistingLineItemImpl(OrderEdit orderEdit, Order order, OrderEditRequest.SetItemQuantity request, LineItem lineItem) {
            super(orderEdit, order, request);

            this.lineItem = lineItem;

            if (!lineItem.isTaxable()) {
                taxSetting = TaxSetting.defaultTax();
                return;
            }

            Set<Integer> productIds = lineItem.getVariantInfo().getProductId() != null
                    ? new HashSet<>(lineItem.getVariantInfo().getProductId())
                    : new HashSet<>();
            this.taxSetting = needTaxes(productIds);
        }

        @Override
        public LineItem lineItem() {
            return this.lineItem;
        }

        @Override
        public TaxSetting taxSetting() {
            return taxSetting;
        }
    }

    public class ResetLineItemQuantity extends ExistingLineItemImpl {

        protected ResetLineItemQuantity(OrderEdit orderEdit, Order order, OrderEditRequest.SetItemQuantity request, LineItem lineItem) {
            super(orderEdit, order, request, lineItem);
        }
    }

    public class DecrementLineItemQuantity extends ExistingLineItemImpl {

        protected DecrementLineItemQuantity(OrderEdit orderEdit, Order order, OrderEditRequest.SetItemQuantity request, LineItem lineItem) {
            super(orderEdit, order, request, lineItem);
        }
    }

    public class IncrementLineItemQuantity extends ExistingLineItemImpl {
        protected IncrementLineItemQuantity(OrderEdit orderEdit, Order order, OrderEditRequest.SetItemQuantity request, LineItem lineItem) {
            super(orderEdit, order, request, lineItem);
        }
    }


    public interface OrderEditAddedLineItem extends SetQuantityContext {
        AddedLineItem lineItem();
    }


    public interface ExistingLineItem extends SetQuantityContext, NeedTax {
        LineItem lineItem();
    }


    public interface SetQuantityContext extends EditContext {

    }

    public interface IncrementContext extends NeedTax, EditContext {
        LineItemInfo lineItemInfo();

        Location location();
    }

    public record LineItemInfo(LineItem lineItem, AddedLineItem addedLineItem) {
    }

    public IncrementContext getIncrementLineItemContext(OrderEditId orderEditId, OrderEditRequest.Increment increment) {
        return new IncrementContextImpl(orderEditId, increment);
    }

    private class IncrementContextImpl extends AbstractContext<OrderEditRequest.Increment>
            implements IncrementContext {

        private final LineItemInfo lineItemInfo;
        private final TaxSetting taxSetting;
        private final Map<Long, Location> locations;

        public IncrementContextImpl(OrderEditId orderEditId, OrderEditRequest.Increment increment) {
            super(orderEditId, increment);

            lineItemInfo = fetchLineItem(increment.getLineItemId());

            Long locationId = increment.getLocationId() == null ? null : increment.getLocationId().longValue();
            locations = needLocations(locationId == null ? List.of() : List.of(locationId),
                    locationId != null);

            Set<Integer> lineItemIds = new HashSet<>();
            lineItemIds.add(0);
            if (lineItemInfo.lineItem != null)
                lineItemIds.add(lineItemInfo.lineItem.getId());
            taxSetting = needTaxes(lineItemIds);
        }

        @Override
        public TaxSetting taxSetting() {
            return taxSetting;
        }

        @Override
        public LineItemInfo lineItemInfo() {
            return lineItemInfo;
        }

        @Override
        public Location location() {
            Long locationId = requst().getLocationId() == null ? null : requst().getLocationId().longValue();
            return locations.getOrDefault(locationId, locations.get(null));
        }
    }

    public interface EditContext {
        OrderEdit orderEdit();
    }

    public interface NeedTax {
        TaxSetting taxSetting();
    }

    private Order fetchOrder(int storeId, int orderId) {
        var order = orderRepository.findById(new OrderId(storeId, orderId));
        if (order == null) {
            throw new ConstrainViolationException("", "");
        }
        return order;
    }

    private OrderEdit fetchOrderEdit(OrderEditId orderEditId) {
        var orderEdit = orderEditRepository.findById(orderEditId);
        if (orderEdit == null) {
            throw new ConstrainViolationException("order_edit", "not found");
        }
        if (orderEdit.isCommitted()) {
            throw new ConstrainViolationException("order_edit", "can not edit order committed");
        }
        return orderEdit;
    }

    private abstract class AbstractContext<T> implements EditContext {
        private final OrderEdit orderEdit;
        private final T request;
        @Nullable
        private Order order;

        @Override
        public OrderEdit orderEdit() {
            return this.orderEdit;
        }

        protected AbstractContext(OrderEdit orderEdit, T request) {
            this.orderEdit = orderEdit;
            this.request = request;
        }

        protected AbstractContext(OrderEditId orderEditId, T request) {
            this.orderEdit = fetchOrderEdit(orderEditId);
            this.request = request;
        }

        protected AbstractContext(OrderEdit orderEdit, Order order, T request) {
            this.orderEdit = orderEdit;
            this.order = order;
            this.request = request;
        }

        private int storeId() {
            return this.orderEdit.getId().getStoreId();
        }

        protected Order order() {
            if (order == null) {
                this.order = fetchOrder(storeId(), this.orderEdit.getOrderId());
            }
            return order;
        }

        protected T requst() {
            return request;
        }

        protected ProductsInfo fetchProductInfo(List<Integer> variantIds) {
            if (CollectionUtils.isEmpty(variantIds)) return new ProductsInfo(Map.of(), Map.of());

            var variantList = productDao.findVariantByListIds(storeId(), variantIds);
            var variants = variantList.stream()
                    .collect(Collectors.toMap(VariantDto::getId, Function.identity()));
            var notFoundVariants = variantIds.stream()
                    .filter(id -> !variants.containsKey(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            if (!notFoundVariants.isEmpty()) {
                throw new ConstrainViolationException("variant_ids",
                        notFoundVariants + "not exited");
            }

            var productIds = variantList.stream()
                    .map(VariantDto::getProductId)
                    .distinct().toList();
            var products = productDao.findProductByListIds(storeId(), productIds).stream()
                    .collect(Collectors.toMap(ProductDto::getId, Function.identity()));

            return new ProductsInfo(products, variants);
        }

        public record ProductsInfo(Map<Integer, ProductDto> products, Map<Integer, VariantDto> variants) {
        }

        protected Map<Long, Location> needLocations(List<Long> locationIds, boolean includedDefaultLocation) {
            Preconditions.checkNotNull(locationIds);

            var builder = LocationFilter.builder()
                    .locationIds(locationIds);
            if (includedDefaultLocation) builder.defaultLocation(true);

            LocationFilter filter = builder.build();
            var responses = sapoClient.locationList(filter).stream()
                    .sorted(Comparator.comparingLong(Location::getId))
                    .toList();
            if (CollectionUtils.isEmpty(responses)) {
                throw new ConstrainViolationException("location", "require location for edit");
            }

            Map<Long, Location> locations = responses.stream()
                    .collect(Collectors.toMap(
                            Location::getId,
                            Function.identity(),
                            (l1, l2) -> throwDuplicateLocation(l1, l2, responses, filter),
                            HashMap::new));

            String notFoundLocations = locationIds.stream()
                    .filter(id -> !locations.containsKey(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            if (!notFoundLocations.isEmpty()) {
                throw new ConstrainViolationException("location", notFoundLocations + "not found");
            }

            if (includedDefaultLocation) {
                var defaultLocations = responses.stream()
                        .filter(Location::isDefaultLocation)
                        .toList();
                Location defaultLocation;
                if (CollectionUtils.isEmpty(defaultLocations)) {
                    defaultLocation = responses.get(0);
                } else {
                    defaultLocation = defaultLocations.get(0);
                }
                locations.put(null, defaultLocation);
            }

            return locations;
        }

        private <T> T throwDuplicateLocation(T l1, T l2, List<T> responses, Object filter) {
            throw new VerifyException("duplicate location");
        }

        protected TaxSetting needTaxes(Set<Integer> productIds) {
            Preconditions.checkNotNull(orderEdit, "order edit must have value before fetched");

            if (order == null) this.order = fetchOrder(storeId(), this.orderEdit.getOrderId());

            var countryCode = OrderEditUtils.getCountryCode(order);
            productIds.add(0);

            return taxHelper.getTaxSetting(storeId(), countryCode, productIds);
        }

        protected LineItemPair fetchLineItemInfo() {
            fetchOrder(storeId(), this.orderEdit.getOrderId());

            Map<Integer, LineItem> lineItems = order().getLineItems().stream()
                    .collect(Collectors.toMap(
                            LineItem::getId,
                            Function.identity()));
            Map<UUID, AddedLineItem> addedLineItems = orderEdit().getLineItems().stream()
                    .collect(Collectors.toMap(
                            AddedLineItem::getId,
                            Function.identity()));

            return new LineItemPair(lineItems, addedLineItems);
        }

        protected LineItemInfo fetchLineItem(String lineItemIdRequest) {
            var lineItemInfo = fetchLineItemInfo();
            Map<Integer, LineItem> lineItems = lineItemInfo.lineItems;
            Map<UUID, AddedLineItem> addedLineItems = lineItemInfo.addedLineItems;

            var lineItemIdPair = OrderEditUtils.resolveLineItemId(lineItemIdRequest);

            LineItem lineItem = null;
            AddedLineItem addedLineItem = null;
            if (lineItemIdPair.getLeft() != null) {
                lineItem = lineItems.get(lineItemIdPair.getKey());
            } else if (lineItemIdPair.getValue() != null) {
                addedLineItem = addedLineItems.get(lineItemIdPair.getValue());
            }
            if (lineItem == null && addedLineItem == null) {
                throw new ConstrainViolationException(
                        "line_item",
                        "line_item not found by id = " + lineItemIdRequest);
            }

            return new LineItemInfo(lineItem, addedLineItem);
        }

        public record LineItemPair(
                Map<Integer, LineItem> lineItems,
                Map<UUID, AddedLineItem> addedLineItems
        ) {
        }
    }


    public interface AddVariantsContext extends EditContext, NeedTax {
        VariantDto getVariant(Integer variantId);

        ProductDto getProduct(Integer productId);

        Location getEffectiveLocation(Integer locationId);
    }

    public AddVariantsContext getAddVariantsContext(OrderEditId editingId, @NotEmpty List<OrderEditRequest.AddVariant> addVariants) {
        return new AddVariantsContextImpl(editingId, addVariants);
    }

    private final class AddVariantsContextImpl extends AbstractContext<List<OrderEditRequest.AddVariant>>
            implements AddVariantsContext {

        private final Map<Integer, VariantDto> variants;
        private final Map<Integer, ProductDto> products;
        private final Map<Long, Location> locations;

        private final TaxSetting taxSetting;

        public AddVariantsContextImpl(OrderEditId editingId, List<OrderEditRequest.AddVariant> addVariants) {
            super(editingId, addVariants);

            var variantIds = addVariants.stream()
                    .map(OrderEditRequest.AddVariant::getVariantId)
                    .filter(NumberUtils::isPositive)
                    .distinct().toList();
            var productInfo = fetchProductInfo(variantIds);
            this.variants = productInfo.variants;
            this.products = productInfo.products;

            this.locations = fetchLocations(addVariants);

            Set<Integer> productIdsNeedTax = variants.values().stream()
                    .filter(VariantDto::isTaxable)
                    .map(VariantDto::getProductId)
                    .filter(NumberUtils::isPositive)
                    .collect(Collectors.toSet());
            taxSetting = needTaxes(productIdsNeedTax);
        }


        private Map<Long, Location> fetchLocations(List<OrderEditRequest.AddVariant> addVariants) {
            boolean anyInvalidLocation = addVariants.stream()
                    .map(OrderEditRequest.AddVariant::getLocationId)
                    .anyMatch(id -> id == null || id <= 0);
            List<Long> locationIds = addVariants.stream()
                    .map(OrderEditRequest.AddVariant::getLocationId)
                    .filter(NumberUtils::isPositive)
                    .map(Long::valueOf)
                    .distinct().toList();
            return needLocations(locationIds, anyInvalidLocation);
        }

        @Override
        public VariantDto getVariant(Integer variantId) {
            return variants.get(variantId);
        }

        @Override
        public ProductDto getProduct(Integer productId) {
            return products.get(productId);
        }

        @Override
        public Location getEffectiveLocation(Integer locationId) {
            return locations.getOrDefault(locationId == null ? null : locationId.longValue(), locations.get(null));
        }

        @Override
        public TaxSetting taxSetting() {
            return this.taxSetting;
        }
    }

}
