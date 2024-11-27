package org.example.order.order.application.service.draftorder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.SapoClient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.ErrorMessage;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.draftorder.request.CombinationCalculateRequest;
import org.example.order.order.application.model.draftorder.response.CalculateProductInfo;
import org.example.order.order.application.model.draftorder.response.CombinationCalculateResponse;
import org.example.order.order.application.model.draftorder.response.LineItemComponent;
import org.example.order.order.application.model.order.context.*;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.TaxHelper;
import org.example.order.order.application.utils.TaxSettingValue;
import org.example.order.order.domain.draftorder.model.VariantType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CombinationCalculateService {

    private final SapoClient sapoClient;
    private final CombinationMapper combinationMapper;
    private final TaxHelper taxHelper;

    public CombinationCalculateResponse calculate(Integer storeId, CombinationCalculateRequest request) {
        validateRequests(request);

        var productInfo = getProductInfo(storeId, request);
        CombinationCalculateResponse calculateResponse;
        if (request.isUpdateProductInfo()) {
            calculateResponse = calculate(request, productInfo);
        }
        return null;
    }

    private CombinationCalculateResponse calculate(CombinationCalculateRequest request, CalculateProductInfo productInfo) {
        var lineItemRequests = request.getLineItems();
        var variantMap = productInfo.getVariantMap();
        var productMap = productInfo.getProductMap();
        var comboMap = productInfo.getComboMap();
        var packsizeMap = productInfo.getPacksizeMap();
        var currency = productInfo.getCurrency();
        var isCalculateTax = request.isCalculateTax();

        List<CombinationCalculateResponse.LineItem> lineItemResponses = new ArrayList<>();
        for (var lineItemRequest : lineItemRequests) {
            var lineItemQuantity = lineItemRequest.getQuantity();
            var discountAllocations = lineItemRequest.getDiscountAllocations();

            var lineItemPrice = lineItemRequest.getLinePrice();

            if (lineItemRequest.getVariantId() == null) {
                var customLineItem = combinationMapper.toResponse(lineItemRequest);
                lineItemPrice = customLineItem.getQuantity().multiply(customLineItem.getPrice());
                customLineItem.setLinePrice(lineItemPrice);
                lineItemResponses.add(customLineItem);
                continue;
            }

            var variant = variantMap.get(lineItemRequest.getVariantId());
            if (variant == null) {
                throw new ConstrainViolationException(UserError.builder()
                        .code("variant_id")
                        .fields(List.of("variant_id"))
                        .build());
            }
            var product = productMap.get(lineItemRequest.getProductId());
            var lineItem = combinationMapper.toResponse(lineItemRequest, variant, product);

            if (lineItemRequest.getPrice() != null && lineItemRequest.getPrice().compareTo(BigDecimal.ZERO) > -1) {
                lineItem.setPrice(lineItemRequest.getPrice());
                lineItemPrice = lineItemQuantity.multiply(lineItemRequest.getPrice());
            } else {
                lineItemPrice = lineItemQuantity.multiply(variant.getPrice());
            }
            lineItem.setLinePrice(lineItemPrice);

            switch (variant.getType()) {
                case packsize -> {
                    var packsize = packsizeMap.get(variant.getId());
                    var childProduct = productMap.get(packsize.getProductId());
                    var childVariant = variantMap.get(packsize.getVariantId());
                    if (childVariant == null) {
                        lineItem.setComponents(new ArrayList<>());
                        lineItemResponses.add(lineItem);
                        continue;
                    }

                    var quantity = packsize.getQuantity().multiply(lineItemQuantity);
                    var componentPrice = lineItemPrice.divide(quantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
                    var remainder = lineItemPrice.subtract(componentPrice.multiply(quantity));
                    var component = combinationMapper.toResponse(
                            childVariant,
                            childProduct,
                            packsize.getQuantity(),
                            remainder,
                            componentPrice,
                            lineItemPrice,
                            lineItemRequest.getDiscountAllocations(),
                            VariantType.packsize
                    );
                    component.setBaseQuantity(packsize.getQuantity());
                    if (NumberUtils.isPositive(remainder)) component.setCanBeOdd(true);
                    lineItem.setComponents(List.of(component));
                    lineItem.setItemUnit(childVariant.getUnit());
                }
                case combo -> {
                    var components = buildComponents(variant, lineItemQuantity, lineItemPrice, discountAllocations, productInfo);
                }
                case normal -> {
                    //
                }
            }
        }
        return null;
    }

    private List<LineItemComponent> buildComponents(
            ProductVariant variant,
            BigDecimal lineItemQuantity,
            BigDecimal lineItemPrice,
            List<CombinationCalculateRequest.ComboPacksizeDiscountAllocation> discountAllocations,
            CalculateProductInfo productInfo
    ) {
        var combo = productInfo.getComboMap().get(variant.getId());
        var comboItems = combo.getComboItems();
        if (CollectionUtils.isEmpty(comboItems)) return new ArrayList<>();
        var originalComboPrice = comboItems.stream()
                .map(item -> item.getPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // các combo item có giá > 0
        var positivePriceLineItems = comboItems.stream().filter(item -> NumberUtils.isPositive(item.getPosition())).toList();
        var positiveCount = positivePriceLineItems.size();
        // các item của combo có giá = 0
        var zeroPriceLineItems = comboItems.stream().filter(item -> !NumberUtils.isPositive(item.getPrice())).toList();
        var zeroCount = zeroPriceLineItems.size();

        var appliedPrice = BigDecimal.ZERO;
        List<LineItemComponent> components = new ArrayList<>();
        var totalRemainder = BigDecimal.ZERO;
        var minQuantity = lineItemQuantity.multiply(comboItems.get(0).getQuantity());
        for (int i = 0; i < positiveCount; i++) {
            var comboItem = positivePriceLineItems.get(i);
            var childProduct = productInfo.getProductMap().get(comboItem.getProductId());
            var component = buildComponent(
                    comboItem,
                    childProduct,
                    originalComboPrice,
                    lineItemQuantity,
                    lineItemPrice,
                    zeroCount,
                    appliedPrice,
                    i == positiveCount - 1,
                    false,
                    productInfo.getCurrency()
            );
            components.add(component);
            appliedPrice = appliedPrice.add(component.getLinePrice()).add(component.getRemainder());
            if (minQuantity.compareTo(component.getQuantity()) > 0) minQuantity = component.getQuantity();
            totalRemainder = totalRemainder.add(component.getRemainder());
        }

        for (int i = 0; i < zeroCount; i++) {
            var comboItem = zeroPriceLineItems.get(i);
            var childProduct = productInfo.getProductMap().get(comboItem.getProductId());
            var component = buildComponent(
                    comboItem,
                    childProduct,
                    originalComboPrice,
                    lineItemQuantity,
                    lineItemPrice,
                    zeroCount,
                    appliedPrice,
                    i == zeroCount - 1,
                    zeroCount == comboItems.size(),
                    productInfo.getCurrency()
            );
            appliedPrice = appliedPrice.add(component.getPrice()).add(component.getRemainder());
            components.add(component);
            if (minQuantity.compareTo(component.getQuantity()) > 0) minQuantity = component.getQuantity();
            totalRemainder = totalRemainder.add(component.getRemainder());
        }

        // sắp xếp giảm dần remainder
        var sortComponents = components.stream().sorted(Comparator.comparing(LineItemComponent::getRemainder).reversed()).toList();
        var canBeOddComponent = addRemainder(totalRemainder, minQuantity, productInfo.getRemainderUnit(), sortComponents, productInfo.getCurrency());
        handleDiscountAllocations(canBeOddComponent, components, sortComponents, discountAllocations, lineItemPrice, minQuantity, productInfo);
        return components;
    }

    private void handleDiscountAllocations(
            LineItemComponent canBeOddComponent,
            List<LineItemComponent> components,
            List<LineItemComponent> sortComponents,
            List<CombinationCalculateRequest.ComboPacksizeDiscountAllocation> discountAllocations,
            BigDecimal lineItemPrice,
            BigDecimal minQuantity,
            CalculateProductInfo productInfo
    ) {
        if (CollectionUtils.isEmpty(discountAllocations)) return;
        var componentCount = components.size();
        var lastLineHasPriceIndex = componentCount - 1;
        for (int i = componentCount - 1; i >= 0; i--) {
            if (NumberUtils.isPositive(components.get(i).getLinePrice())) {
                lastLineHasPriceIndex = i;
                break;
            }
        }

        var currency = productInfo.getCurrency();
        for (var discountAllocation : discountAllocations) {
            var appliedDiscountAmount = BigDecimal.ZERO;
            var totalRemainder = BigDecimal.ZERO;
            for (int i = 0; i < componentCount; i++) {
                var component = components.get(i);
                var splitAmount = component.getLinePrice()
                        .multiply(discountAllocation.getAmount())
                        .divide(lineItemPrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);

                appliedDiscountAmount = appliedDiscountAmount.add(splitAmount);
                var amount = splitAmount.min(component.getSubtotal())
                        .divide(component.getQuantity(), currency.getDefaultFractionDigits(), RoundingMode.FLOOR)
                        .multiply(component.getQuantity());
                var remainder = splitAmount.subtract(amount);
                totalRemainder = totalRemainder.add(remainder);
                var allocation = discountAllocation.toBuilder()
                        .amount(amount)
                        .remainder(remainder)
                        .build();
                component.addDiscountAllocations(discountAllocation);
//                component.setRemainder();
            }
        }
    }

    private LineItemComponent addRemainder(
            BigDecimal totalRemainder,
            BigDecimal minQuantity,
            BigDecimal remainderUnit,
            List<LineItemComponent> sortComponents,
            Currency currency
    ) {
        if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0) return sortComponents.get(0);

        for (var component : sortComponents) {
            var addPrice = component.getQuantity().multiply(remainderUnit);
            if (addPrice.compareTo(totalRemainder) <= 0) {
                component.setLinePrice(component.getLinePrice().add(addPrice));
                component.setPrice(component.getPrice().add(remainderUnit));
                totalRemainder = totalRemainder.subtract(addPrice);
            }
        }
        return null;
    }

    private LineItemComponent buildComponent(
            ComboItem comboItem,
            Product childProduct,
            BigDecimal originalComboPrice,
            BigDecimal lineItemQuantity,
            BigDecimal lineItemPrice,
            int zeroCount,
            BigDecimal appliedPrice,
            boolean isLastItem,
            boolean isAvgSplit,
            Currency currency
    ) {
        var componentQuantity = comboItem.getQuantity().multiply(lineItemQuantity);
        var componentBuilder = LineItemComponent.builder()
                .variantId(comboItem.getVariantId())
                .sku(comboItem.getSku())
                .grams(comboItem.getGrams())
                .title(childProduct.getName())
                .taxable(comboItem.isTaxable())
                .variantTitle(comboItem.getTitle())
                .inventoryManagement(comboItem.getInventoryManagement())
                .inventoryPolicy(comboItem.getInventoryPolicy())
                .inventoryItemId(comboItem.getInventoryItemId())
                .vendor(childProduct.getVendor())
                .productId(childProduct.getId())
                .quantity(componentQuantity)
                .source(VariantType.combo)
                .baseQuantity(comboItem.getQuantity())
                .requiresShipping(comboItem.isRequiresShipping())
                .unit(comboItem.getUnit());

        if (!isLastItem) {
            var splitPrice = comboItem.getPrice().multiply(comboItem.getQuantity())
                    .multiply(lineItemPrice)
                    .divide(originalComboPrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
            var componentPrice = splitPrice.divide(componentQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            var componentLinePrice = componentPrice.multiply(componentQuantity);
            var remainder = splitPrice.subtract(componentLinePrice);
            return componentBuilder
                    .linePrice(componentLinePrice)
                    .subtotal(componentLinePrice)
                    .price(componentPrice)
                    .remainder(remainder)
                    .build();
        }

        var splitPrice = lineItemPrice.subtract(appliedPrice);
        var componentPrice = splitPrice.divide(componentQuantity, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        var componentLinePrice = componentPrice.multiply(componentQuantity);
        var remainder = splitPrice.subtract(componentLinePrice);
        return componentBuilder
                .price(componentPrice)
                .linePrice(componentLinePrice)
                .subtotal(componentLinePrice)
                .remainder(remainder)
                .build();
    }

    private CalculateProductInfo getProductInfo(Integer storeId, CombinationCalculateRequest request) {
        var productInfo = CalculateProductInfo.builder();
        var baseVariantIds = request.getLineItems().stream().map(CombinationCalculateRequest.LineItem::getVariantId).filter(NumberUtils::isPositive).distinct().toList();
        if (CollectionUtils.isEmpty(baseVariantIds)) return productInfo.build();

        var baseVariants = sapoClient.productVariantFilter(storeId, baseVariantIds);

        var allProductIds = baseVariants.stream().map(ProductVariant::getProductId).distinct().collect(Collectors.toList());

        var variantMap = baseVariants.stream().collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        var comboIds = baseVariants.stream()
                .filter(variant -> variant.getType() == VariantType.combo)
                .map(ProductVariant::getId)
                .distinct().toList();
        var packsizeIds = baseVariants.stream()
                .filter(variant -> variant.getType() == VariantType.packsize)
                .map(ProductVariant::getId)
                .distinct().toList();

        if (CollectionUtils.isNotEmpty(comboIds)) {
            var combos = sapoClient.comboFilter(storeId, comboIds);
            productInfo.comboMap(combos.stream().collect(Collectors.toMap(Combo::getVariantId, Function.identity())));
            Map<Integer, ProductVariant> comboVariantMap = new HashMap<>();
            combos.stream().flatMap(c -> c.getComboItems().stream())
                    .forEach(item -> comboVariantMap.putIfAbsent(item.getVariantId(), combinationMapper.toResponse(item)));
            variantMap.putAll(comboVariantMap);
            allProductIds.addAll(comboVariantMap.values().stream().map(ProductVariant::getProductId).distinct().toList());
        }

        if (CollectionUtils.isNotEmpty(packsizeIds)) {
            var packsizes = sapoClient.packsizeFilterByVariantIds(storeId, packsizeIds);
            var packsizeVariantIds = packsizes.stream().map(Packsize::getVariantId).distinct().toList();
        }

        var productMap = sapoClient.productFilter(storeId, allProductIds.stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        var currency = Currency.getInstance(request.getCurrency());
        var remainderUnit = BigDecimal.ONE.movePointLeft(currency.getDefaultFractionDigits());

        if (request.isCalculateTax()) {
            Set<Integer> productIds = new HashSet<>(productMap.keySet());
            productIds.add(0);
            var taxSetting = taxHelper.getTaxSetting(storeId, request.getCountryCode(), productIds);
            var allTax = taxSetting.getTaxes();
            var productTaxes = allTax.stream()
                    .filter(item -> NumberUtils.isPositive(item.getProductId()))
                    .collect(Collectors.toMap(TaxSettingValue::getProductId, Function.identity()));
            var countryTax = allTax.stream()
                    .filter(t -> t.getTaxType() == null && t.getProductId() == null)
                    .findFirst().orElse(TaxSettingValue.builder().rate(BigDecimal.ZERO).build());
            productInfo
                    .productTaxMap(productTaxes)
                    .countryTaxSetting(countryTax);
        }

        return productInfo
                .productMap(productMap)
                .variantMap(variantMap)
                .currency(currency)
                .remainderUnit(remainderUnit)
                .build();
    }

    private void validateRequests(CombinationCalculateRequest request) {
        var errors = new ArrayList<UserError>();
        for (int i = 0; i < request.getLineItems().size(); i++) {
            var lineItem = request.getLineItems().get(i);
            if (!NumberUtils.isPositive(lineItem.getVariantId()) && (lineItem.getPrice() == null || lineItem.getPrice().signum() < 0)) {
                errors.add(UserError.builder()
                        .code(String.format("line_items[%s].price", i)).message("price must be greater than or equal to 0")
                        .fields(List.of("line_items", String.valueOf(i), "price"))
                        .build());
            }
            if (!request.isUpdateProductInfo() && (CollectionUtils.isNotEmpty(lineItem.getComponents()) && lineItem.getType() == VariantType.normal)) {
                errors.add(UserError.builder()
                        .code(String.format("line_items[%s].type", i)).message("type must be combo or packsize")
                        .fields(List.of("line_items", String.valueOf(i), "type"))
                        .build());

            }
        }
        if (CollectionUtils.isNotEmpty(errors)) {
            var errorMessageBuilder = ErrorMessage.builder();
            for (UserError error : errors) {
                errorMessageBuilder.addError(error);
            }
            throw new ConstrainViolationException(errorMessageBuilder.build());
        }
    }
}
