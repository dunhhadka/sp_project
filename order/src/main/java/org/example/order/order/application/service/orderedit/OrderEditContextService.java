package org.example.order.order.application.service.orderedit;

import com.google.common.base.Preconditions;
import com.google.common.base.VerifyException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.SapoClient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.model.order.request.LocationFilter;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.TaxHelper;
import org.example.order.order.application.utils.TaxSetting;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;
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

    public interface EditContext {
        OrderEdit orderEdit();
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

        protected AbstractContext(OrderEditId orderEditId, T request) {
            this.orderEdit = fetchOrderEdit(orderEditId);
            this.request = request;
        }

        private int storeId() {
            return this.orderEdit.getId().getStoreId();
        }

        private void fetchOrder() {
            Preconditions.checkNotNull(orderEdit, "require not null");

            var order = orderRepository.findById(new OrderId(storeId(), orderEdit.getOrderId()));
            if (order == null) {
                throw new ConstrainViolationException("", "");
            }

            this.order = order;
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

            if (order == null) fetchOrder();

            var countryCode = OrderEditUtils.getCountryCode(order);
            productIds.add(0);

            return taxHelper.getTaxSetting(storeId(), countryCode, productIds);
        }
    }


    public interface AddVariantsContext extends EditContext {
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
    }
}
