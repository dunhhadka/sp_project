package org.example.order.order.application.service.draftorder;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.ErrorMessage;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.draftorder.request.*;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.SupportCurrencies;
import org.example.order.order.application.utils.TaxHelper;
import org.example.order.order.domain.draftorder.model.*;
import org.example.order.order.domain.draftorder.persistence.DraftOrderIdGenerator;
import org.example.order.order.domain.draftorder.persistence.NumberGenerator;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dao.StoreDao;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.StoreDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DraftOrderWriteService {

    private final StoreDao storeDao;
    private final ProductDao productDao;
    private final NumberGenerator numberGenerator;
    private final TaxHelper taxHelper;
    private final DraftOrderIdGenerator idGenerator;

    private final DratOrderMapper dratOrderMapper;
    private final CombinationCalculateService combinationCalculateService;

    @Transactional

    public DraftOrderId createDraftOrder(Integer storeId, DraftOrderCreateRequest request) {
        var store = getStoreById(storeId);

        var currency = getCurrency(store, request.getCurrency());
        var draftOrderId = new DraftOrderId(storeId, idGenerator.generateDraftOrderId());

        var draftOrder = new DraftOrder(
                draftOrderId,
                numberGenerator,
                request.getCopyOrderId(),
                taxHelper,
                currency
        );
        var lineItems = buildLineItems(storeId, currency, request.getLineItems());
        draftOrder.setLineItems(lineItems);

        return createOrUpdateDraftOrder(storeId, request, draftOrder);
    }

    private DraftOrderId createOrUpdateDraftOrder(Integer storeId, DraftOrderCreateRequest request, DraftOrder draftOrder) {
        buildDraftOrder(storeId, request, draftOrder);
        draftOrder.setModifiedId(null);
        return draftOrder.getId();
    }

    private void buildDraftOrder(Integer storeId, DraftOrderCreateRequest request, DraftOrder draftOrder) {
        var draftOrderInfoBuilder = draftOrder.getDraftOrderInfo().toBuilder();

        draftOrderInfoBuilder
                .note(request.getNote())
                .email(request.getEmail())
                .phone(request.getPhone())
                .taxExempt(request.getTaxExempt());

        draftOrder.overrideTags(request.getTags());

        draftOrder.setDraftOrderInfo(draftOrderInfoBuilder.build());

        if (CollectionUtils.isNotEmpty(request.getLineItems())) {
            this.handleCombinations(storeId, draftOrder);
        }
    }

    private void handleCombinations(Integer storeId, DraftOrder draftOrder) {
        var draftLineItems = draftOrder.getLineItems();
        var currency = draftOrder.getDraftOrderInfo().getCurrency();
        if (draftLineItems.stream().anyMatch(line -> line.getProductInfo().getType() != VariantType.normal)) {
            var errors = new ArrayList<UserError>();

            var combinationCalculateRequest = CombinationCalculateRequest.builder()
                    .calculateTax(false)
                    .updateProductInfo(true)
                    .currency(currency.getCurrencyCode())
                    .lineItems(dratOrderMapper.toCombinationCalculateLine(draftLineItems))
                    .build();

            var calculateResponse = combinationCalculateService.calculate(storeId, combinationCalculateRequest);
        }
    }

    private List<DraftOrderLineItem> buildLineItems(int storeId, Currency currency, List<DraftLineItemRequest> lineItemRequests) {
        this.validateLineItems(lineItemRequests);

        var variantIds = lineItemRequests.stream()
                .map(DraftLineItemRequest::getVariantId)
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();
        var variants = productDao.findVariantByListIds(storeId, variantIds);
        var productIds = variants.stream()
                .map(VariantDto::getProductId)
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();
        var products = productDao.findProductByListIds(storeId, productIds);

        List<DraftOrderLineItem> lineItems = new ArrayList<>();
        List<DraftDiscountApplication> discountApplications = new ArrayList<>();
        for (var lineReq : lineItemRequests) {
            VariantDto variant;
            ProductDto product = null;
            if (!lineReq.isCustom()) {
                variant = variants.stream()
                        .filter(v -> Objects.equals(v.getId(), lineReq.getVariantId()))
                        .findFirst().orElse(null);
                if (variant != null) {
                    product = products.stream()
                            .filter(p -> Objects.equals(p.getId(), variant.getProductId()))
                            .findFirst().orElse(null);
                }
            } else {
                variant = null;
            }
            lineItems.add(buildLineItem(currency, variant, product, lineReq, discountApplications));
        }
        return lineItems;
    }

    private DraftOrderLineItem buildLineItem(
            Currency currency, VariantDto variant,
            ProductDto product, DraftLineItemRequest request,
            List<DraftDiscountApplication> discountApplications
    ) {
        boolean isCustom = request.isCustom();
        Integer variantId = request.getVariantId();
        Integer productId = request.getProductId();
        String inventoryManagement = null;
        Long inventoryItemId = null;
        String title = request.getTitle();
        String variantTitle = request.getVariantTitle();
        BigDecimal price = request.getPrice();
        String sku = request.getSku();
        boolean taxable = request.isTaxable();
        String vendor = request.getVendor();
        String unit = null;
        String itemUnit = null;
        int quantity = request.getQuantity();
        int grams = request.getGrams();
        boolean requireShipping = request.getRequireShipping();
        VariantType type = null;

        if (variant != null && product != null) {
            variantId = variant.getId();
            productId = product.getId();
            inventoryManagement = variant.getInventoryManagement();
            inventoryItemId = variant.getInventoryItemId();

            if (StringUtils.isBlank(title)) title = product.getName();
            if (StringUtils.isBlank(variantTitle)) variantTitle = variant.getTitle();
            if (StringUtils.isBlank(vendor)) vendor = product.getVendor();
            sku = variant.getSku();

            if (!taxable) taxable = variant.isTaxable();
            if (!requireShipping) requireShipping = variant.isRequiresShipping();

            unit = variant.getUnit();
            itemUnit = variant.getUnit();

            type = VariantType.valueOf(variant.getType().name());
        }

        DraftProductInfo productInfo = DraftProductInfo.builder()
                .variantId(variantId)
                .inventoryItemId(inventoryItemId)
                .inventoryManagement(inventoryManagement)
                .productId(productId)
                .title(title)
                .variantTitle(variantTitle)
                .price(price)
                .sku(sku)
                .taxable(taxable)
                .vendor(vendor)
                .unit(unit)
                .itemUnit(itemUnit)
                .type(type)
                .build();

        var properties = buildPropeties(request.getProperties());

        DraftAppliedDiscount appliedDiscount = getAppliedDiscount(request.getAppliedDiscount());

        var lineItem = new DraftOrderLineItem(
                isCustom,
                productInfo,
                quantity,
                grams,
                requireShipping,
                appliedDiscount,
                properties,
                currency
        );

        if (appliedDiscount != null) {
            var discountInfo = resolveDiscount(lineItem, discountApplications);
            if (discountInfo != null) {
                lineItem.setDiscountAllocations(List.of(discountInfo.getValue()));
            }
        }

        return lineItem;
    }

    private Pair<DraftDiscountApplication, DraftDiscountAllocation> resolveDiscount(DraftOrderLineItem lineItem, List<DraftDiscountApplication> discountApplications) {
        if (lineItem.getAppliedDiscount() == null) return null;
        var appliedDiscount = lineItem.getAppliedDiscount();
        DraftDiscountApplication application = DraftDiscountApplication.builder()
                .index(discountApplications.size())
                .code(appliedDiscount.getCode())
                .title(appliedDiscount.getTitle())
                .description(appliedDiscount.getDescription())
                .value(appliedDiscount.getValue())
                .maxValue(appliedDiscount.getValue())
                .amount(appliedDiscount.getAmount())
                .type(DiscountApplication.ValueType.valueOf(appliedDiscount.getValueType().name()))
                .targetType(DiscountApplication.TargetType.line_item)
                .build();
        DraftDiscountAllocation allocation = DraftDiscountAllocation.builder()
                .amount(appliedDiscount.getAmount())
                .discountApplicationIndex(application.getIndex())
                .build();

        discountApplications.add(application);

        return Pair.of(application, allocation);
    }

    private DraftAppliedDiscount getAppliedDiscount(DraftAppliedDiscountRequest appliedDiscount) {
        if (appliedDiscount == null) return null;
        return DraftAppliedDiscount.builder()
                .title(appliedDiscount.getTitle())
                .description(appliedDiscount.getDescription())
                .value(appliedDiscount.getValue())
                .valueType(appliedDiscount.getValueType())
                .custom(appliedDiscount.isCustom())
                .amount(BigDecimal.ZERO) // xử lý sau
                .build();
    }

    private List<DraftProperty> buildPropeties(List<DraftPropertyRequest> properties) {
        if (CollectionUtils.isEmpty(properties)) return List.of();
        return properties.stream()
                .map(p -> DraftProperty.builder()
                        .name(p.getName())
                        .value(p.getValue())
                        .build())
                .collect(Collectors.toMap(DraftProperty::getName, Function.identity(), (a, b) -> b))
                .values().stream().toList();
    }

    private void validateLineItems(List<DraftLineItemRequest> lineItems) {
        List<UserError> userErrors = new ArrayList<>();
        for (int i = 0; i < lineItems.size(); i++) {
            var lineItem = lineItems.get(i);
            if (lineItem.isCustom() && StringUtils.isBlank(lineItem.getTitle())) {
                userErrors.add(UserError.builder()
                        .fields(List.of("title"))
                        .code("not_blank")
                        .message(String.format("line_items[%s].title is blank", i))
                        .build());
            }
            if (lineItem.isCustom() && (lineItem.getPrice() == null || lineItem.getPrice().signum() < 0)) {
                userErrors.add(UserError.builder()
                        .fields(List.of("price"))
                        .code("invalid")
                        .message(String.format("line_items[%s].price invalid", i))
                        .build());
            }
        }
        if (CollectionUtils.isNotEmpty(userErrors)) {
            throw new ConstrainViolationException(ErrorMessage.builder()
                    .addErrors(userErrors)
                    .build());
        }
    }

    private Currency getCurrency(StoreDto store, String currencyRequest) {
        if (StringUtils.isNotBlank(currencyRequest)) {
            if (!Order.DEFAUT_CURRENCY.getCurrencyCode().equals(currencyRequest)) {
                var currency = SupportCurrencies.getByCode(currencyRequest);
                if (currency == null) {
                    throw new ConstrainViolationException("currency", "not supported");
                }
                return Currency.getInstance(currency.getCode());
            }
            return Order.DEFAUT_CURRENCY;
        }
        return store.getCurrency() != null ? store.getCurrency() : Order.DEFAUT_CURRENCY;
    }


    private StoreDto getStoreById(Integer storeId) {
        var store = storeDao.findById(storeId);
        if (store != null) return store;
        throw new ConstrainViolationException("store", "store not found by id = " + storeId);
    }
}
