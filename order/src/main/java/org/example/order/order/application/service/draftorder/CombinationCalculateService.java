package org.example.order.order.application.service.draftorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.SapoClient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.ErrorMessage;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.draftorder.request.CombinationCalculateRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CombinationCalculateService {

    private final SapoClient sapoClient;
    private final CombinationMapper combinationMapper;
    private final TaxHelper taxHelper;

    public CombinationCalculateResponse calculate(Integer storeId, CombinationCalculateRequest request) {
        validateRequest(request);

        var productInfo = getProductInfo(storeId, request);

        CombinationCalculateResponse result;
        if (request.isUpdateProductInfo()) { // update product
            result = calculate(request, productInfo);
        } else {
            result = calculateWithoutUpdateProductInfo(request, productInfo);
        }
        return result;
    }

    private CombinationCalculateResponse calculateWithoutUpdateProductInfo(CombinationCalculateRequest request, CalculateProductInfo productInfo) {
        var lineItems = new ArrayList<CombinationCalculateResponse.LineItem>();
        for (var lineItem : request.getLineItems()) {
            var lineItemResponse = combinationMapper.toResponse(lineItem);
            var components = lineItemResponse.getComponents();
            if (CollectionUtils.isNotEmpty(components)) {
                var lineItemPrice = lineItemResponse.getLinePrice();
                var minQuantity = components.stream()
                        .map(CombinationCalculateResponse.LineItemComponent::getQuantity)
                        .min(Comparator.naturalOrder())
                        .orElse(productInfo.getRemainderUnit());
                for (var component : components) {
                    component.setSubtotal(component.getLinePrice());
                }
                var canBeOddComponent = components.stream()
                        .filter(CombinationCalculateResponse.LineItemComponent::isCanBeOdd)
                        .findFirst()
                        .orElse(components.get(0));
                var sortedComponents = components.stream()
                        .sorted(Comparator.comparing(CombinationCalculateResponse.LineItemComponent::getRemainder).reversed())
                        .toList();
                handleDiscountAllocations(canBeOddComponent, components, sortedComponents, lineItemResponse.getDiscountAllocations(), lineItemPrice, minQuantity, productInfo);
            }
            if (request.isCalculateTax()) {
                handleTaxLines(lineItemResponse, productInfo.getCountryTaxSetting(), productInfo.getCurrency(), request.isTaxIncluded(), request.isTaxExempt(), productInfo);
            }
            lineItems.add(lineItemResponse);
        }
        return CombinationCalculateResponse.builder()
                .lineItems(lineItems)
                .build();
    }

    private void handleTaxLines(CombinationCalculateResponse.LineItem lineItem, TaxSettingValue countryTaxSetting, Currency currency, boolean taxIncluded, boolean isTaxExempt, CalculateProductInfo productInfo) {
        List<CombinationCalculateResponse.ComboPacksizeTaxLine> taxLines = new ArrayList<>();
        Map<String, CombinationCalculateResponse.ComboPacksizeTaxLine> taxLineMap = new HashMap<>();
        var isCustomTax = CollectionUtils.isNotEmpty(lineItem.getTaxLines());
        if (isCustomTax) {
            // Merge rồi chia thuế về các line thành phần
            lineItem.setTaxLines(taxLines);
        }
        switch (lineItem.getType()) {
            case packsize, combo -> {
                var components = lineItem.getComponents();
                if (isCustomTax) {
                    var count = components.size();
                    for (CombinationCalculateResponse.ComboPacksizeTaxLine taxLine : taxLines) {
                        var appliedTax = BigDecimal.ZERO;
                        for (int i = 0; i < count; i++) {
                            var component = components.get(i);
                            if (i < count - 1) {
//                                var price = TaxUtils.distribute(taxLine.getPrice(), lineItem.getLinePrice(), component.getLinePrice(), currency);
                                var price = BigDecimal.ZERO;
                                appliedTax = appliedTax.add(price);
                                var customTaxLine = CombinationCalculateResponse.ComboPacksizeTaxLine.builder()
                                        .rate(taxLine.getRate())
                                        .title(taxLine.getTitle())
                                        .price(price)
                                        .build();
                                component.addTaxLine(customTaxLine);
                                continue;
                            }
                            component.addTaxLine(CombinationCalculateResponse.ComboPacksizeTaxLine.builder()
                                    .rate(taxLine.getRate())
                                    .title(taxLine.getTitle())
                                    .price(taxLine.getPrice().subtract(appliedTax))
                                    .build());
                        }
                    }
                } else if (!isTaxExempt) {
                    for (CombinationCalculateResponse.LineItemComponent component : components) {
                        if (!component.isTaxable()) continue;
                        var productTax = productInfo.getProductTaxMap().get((int) component.getProductId());
//                        var taxLine = TaxUtils.buildTaxLine(productTax, countryTax, component.getSubtotalPrice(), currency, taxesIncluded);
                        CombinationCalculateResponse.ComboPacksizeTaxLine taxLine = null;
                        List<CombinationCalculateResponse.ComboPacksizeTaxLine> componentTaxLines = new ArrayList<>();
                        componentTaxLines.add(taxLine);
                        component.setTaxLines(componentTaxLines);
//                        TaxUtils.merge(taxLineMap, taxLine);
                    }
                    lineItem.setTaxLines(taxLineMap.values().stream().toList());
                }
            }
            case normal -> {
                if (!isCustomTax && !isTaxExempt && lineItem.isTaxable()) {
//                    var productTax = NumberUtils.isPositive(lineItem.getProductId()) ? productInfo.productTaxMap.get(lineItem.getProductId().intValue()) : null;
//                    taxLines.add(TaxUtils.buildTaxLine(productTax, countryTax, lineItem.getSubtotalPrice(), currency, taxesIncluded));
                    lineItem.setTaxLines(taxLines);
                }
            }
        }
    }

    private CombinationCalculateResponse calculate(CombinationCalculateRequest request, CalculateProductInfo productInfo) {
        var lineItemRequests = request.getLineItems();
        var variantMap = productInfo.getVariantMap();
        var productMap = productInfo.getProductMap();
        var packsizeMap = productInfo.getPacksizeMap();
        var currency = productInfo.getCurrency();
        var isCalculateTax = request.isCalculateTax();

        List<CombinationCalculateResponse.LineItem> lineResponses = new ArrayList<>();
        for (var lineItemRequest : lineItemRequests) {
            var lineItemQuantity = lineItemRequest.getQuantity();
            var discountAllocations = lineItemRequest.getDiscountAllocations();
            // Giá của line sản phẩm gôc
            var lineItemPrice = lineItemRequest.getLinePrice();
            if (lineItemRequest.getVariantId() == null) {
                var customLineItem = combinationMapper.toResponse(lineItemRequest);
                lineItemPrice = customLineItem.getQuantity().multiply(customLineItem.getPrice());
                customLineItem.setLinePrice(lineItemPrice);
                lineResponses.add(customLineItem);
                continue;
            }
            var variant = variantMap.get(lineItemRequest.getVariantId());
            if (variant == null) {
                throw new ConstrainViolationException(UserError.builder()
                        .code("variant_id")
                        .message("variant_id = %s is not exist".formatted(lineItemRequest.getVariantId()))
                        .fields(List.of("variant_id"))
                        .build());
            }
            var product = productMap.get(variant.getProductId());
            var lineItem = combinationMapper.toResponse(lineItemRequest, variant, product);

            //Note: Nếu có cả variantId và cả price thì ưu tin lấy giá trị truyền vào
            if (lineItemRequest.getPrice() != null && lineItemRequest.getPrice().compareTo(BigDecimal.ZERO) > -1) {
                lineItem.setPrice(lineItemRequest.getPrice());
                lineItemPrice = lineItemQuantity.multiply(lineItemRequest.getPrice());
            } else {
                lineItemPrice = lineItemQuantity.multiply(variant.getPrice());
            }
            lineItem.setLinePrice(lineItemPrice);

            // build lại các thành phần component của lineItem
            switch (variant.getType()) {
                case packsize -> {
                    var packsize = packsizeMap.get(variant.getId());
                    var childProduct = productMap.get(packsize.getProductId());
                    var childVariant = variantMap.get(packsize.getVariantId());
                    if (childVariant == null) {
                        log.debug("Packsize variant not found");
                        lineItem.setComponents(List.of());
                        lineResponses.add(lineItem);
                        continue;
                    }
                    // Tổng số quantity của packsize: VD. 1 packsize A có 15 quantity => 1 line có 15 quantity
                    var quantity = packsize.getQuantity().multiply(lineItemQuantity);
                    var componentPrice = lineItemPrice.divide(quantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
                    // phần dư sau khi chia
                    var remainder = lineItemPrice.subtract(componentPrice.multiply(quantity));
                    var component = combinationMapper.toResponse(
                            childVariant,
                            childProduct,
                            quantity,
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
                    var components = buildComboComponents(variant, lineItemQuantity, lineItemPrice, discountAllocations, productInfo);
                    lineItem.setComponents(components);
                }
                case normal -> {
                    //NOTE: Giữ nguyên
                }
                default -> throw new IllegalArgumentException();
            }
        }

        return CombinationCalculateResponse.builder()
                .lineItems(lineResponses)
                .build();
    }

    private List<CombinationCalculateResponse.LineItemComponent> buildComboComponents(
            ProductVariant variant,
            BigDecimal lineItemQuantity,
            BigDecimal lineItemPrice,
            List<CombinationCalculateResponse.ComboPacksizeDiscountAllocation> discountAllocations,
            CalculateProductInfo productInfo
    ) {
        var combo = productInfo.getComboMap().get(variant.getId());
        var comboItems = combo.getComboItems();
        if (CollectionUtils.isEmpty(comboItems)) return new ArrayList<>();

        // giá gốc của combo
        var originalComboPrice = comboItems.stream()
                .map(item -> item.getPrice().multiply(item.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Tách thành 2 loại: item có price > 0 và item có price == 0;
        var positivePriceLineItems = comboItems.stream().filter(item -> NumberUtils.isPositive(item.getPrice())).toList();
        var zeroPriceLineItems = comboItems.stream().filter(item -> item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) == 0).toList();
        var positiveCount = positivePriceLineItems.size();
        var zeroCount = zeroPriceLineItems.size();

        var appliedPrice = BigDecimal.ZERO;
        List<CombinationCalculateResponse.LineItemComponent> components = new ArrayList<>();
        var totalRemainder = BigDecimal.ZERO;
        var minQuantity = lineItemQuantity.multiply(comboItems.get(0).getQuantity());
        for (int i = 0; i < positiveCount; i++) {
            var comboItem = positivePriceLineItems.get(i);
            var childProduct = productInfo.getProductMap().get(comboItem.getProductId());
            var component = buildComboComponent(
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
            appliedPrice = appliedPrice.add(component.getLinePrice()).add(component.getRemainder());
            components.add(component);
            if (minQuantity.compareTo(component.getQuantity()) > 0) minQuantity = component.getQuantity();
            totalRemainder = totalRemainder.add(component.getRemainder());
        }
        for (int i = 0; i < zeroCount; i++) {
            var comboItem = zeroPriceLineItems.get(i);
            var childProduct = productInfo.getProductMap().get(comboItem.getProductId());
            var component = buildComboComponent(
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
            appliedPrice = appliedPrice.add(component.getLinePrice()).add(component.getRemainder());
            components.add(component);
            if (minQuantity.compareTo(component.getQuantity()) > 0) minQuantity = component.getQuantity();
            totalRemainder = totalRemainder.add(component.getRemainder());
        }

        // sort theo thứ tự giảm dần của remainder
        var sortComponents = components.stream().sorted(Comparator.comparing(CombinationCalculateResponse.LineItemComponent::getRemainder).reversed()).toList();
        var canBeOddComponent = addRemainder(totalRemainder, minQuantity, productInfo.getRemainderUnit(), sortComponents, productInfo.getCurrency());
        handleDiscountAllocations(canBeOddComponent, components, sortComponents, discountAllocations, lineItemPrice, minQuantity, productInfo);
        return components;
    }

    private void handleDiscountAllocations(
            CombinationCalculateResponse.LineItemComponent canBeOddComponent,
            List<CombinationCalculateResponse.LineItemComponent> components,
            List<CombinationCalculateResponse.LineItemComponent> sortComponents,
            List<CombinationCalculateResponse.ComboPacksizeDiscountAllocation> discountAllocations,
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
        // chỉ phân bổ discount về các line có price > 0;
        for (var discountAllocation : discountAllocations) {
            var appliedDiscountAmount = BigDecimal.ZERO;
            var totalRemainder = BigDecimal.ZERO;
            for (int i = 0; i < componentCount; i++) {
                var component = components.get(i);
                var splitAmount = lastLineHasPriceIndex == i
                        ? discountAllocation.getAmount().subtract(appliedDiscountAmount)
                        : lineItemPrice.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO
                        : component.getLinePrice().multiply(discountAllocation.getAmount())
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
                component.addDiscountAllocation(allocation);
                component.setRemainderDiscountAllocation(allocation);
            }
            var sortedComponentsByDiscountRemainder = components.stream()
                    .sorted(Comparator.comparing((CombinationCalculateResponse.LineItemComponent lineItemComponent) ->
                                    lineItemComponent.getRemainderDiscountAllocation().getAmount())
                            .reversed())
                    .toList();
            addRemainderForDiscountAllocations(canBeOddComponent, sortedComponentsByDiscountRemainder, sortComponents, totalRemainder, minQuantity, productInfo.getRemainderUnit());
        }
    }

    private void addRemainderForDiscountAllocations(
            CombinationCalculateResponse.LineItemComponent canBeOddComponent,
            List<CombinationCalculateResponse.LineItemComponent> sortedComponentsByDiscountRemainder,
            List<CombinationCalculateResponse.LineItemComponent> sortComponents,
            BigDecimal totalRemainder,
            BigDecimal minQuantity,
            BigDecimal remainderUnit
    ) {
        if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0) return;

        for (var component : sortedComponentsByDiscountRemainder) {
            var discountAllocation = component.getRemainderDiscountAllocation();
            var addPrice = component.getQuantity().multiply(remainderUnit);
            var newSubtotal = component.getSubtotal().subtract(addPrice);
            if (addPrice.compareTo(totalRemainder) <= 0 && newSubtotal.compareTo(BigDecimal.ZERO) >= 0) {
                discountAllocation.addAmount(addPrice);
                totalRemainder = totalRemainder.subtract(addPrice);
                component.setSubtotal(newSubtotal);
            }
            if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0)
                return;
            if (totalRemainder.compareTo(minQuantity.multiply(remainderUnit)) < 0) break;
        }

        var oddComponentDiscount = canBeOddComponent.getRemainderDiscountAllocation();
        var addPrice = totalRemainder.min(canBeOddComponent.getSubtotal());
        oddComponentDiscount.addAmount(addPrice);
        totalRemainder = totalRemainder.subtract(addPrice);
        canBeOddComponent.setSubtotal(canBeOddComponent.getSubtotal().subtract(addPrice));

        if (totalRemainder.compareTo(BigDecimal.ZERO) > 0) {
            for (var component : sortComponents) {
                var discountAllocation = component.getRemainderDiscountAllocation();
                var addPriceFinal = component.getSubtotal().min(totalRemainder);
                var newSubtotal = component.getSubtotal().subtract(addPriceFinal);
                discountAllocation.addAmount(addPriceFinal);
                component.setSubtotal(newSubtotal);
                totalRemainder = totalRemainder.subtract(addPrice);
                if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0) return;
            }
        }
    }


    private CombinationCalculateResponse.LineItemComponent addRemainder(
            BigDecimal totalRemainder,
            BigDecimal minQuantity,
            BigDecimal remainderUnit,
            List<CombinationCalculateResponse.LineItemComponent> components,
            Currency currency
    ) {
        if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0) return components.get(0);
        for (var component : components) {
            var addPrice = component.getQuantity().multiply(remainderUnit);
            if (addPrice.compareTo(totalRemainder) <= 0) {
                component.setLinePrice(component.getLinePrice().add(addPrice));
                component.setPrice(component.getPrice().add(remainderUnit));
                totalRemainder = totalRemainder.subtract(addPrice);
            }
            if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0) {
                component.setCanBeOdd(true);
                return component;
            }
            if (totalRemainder.compareTo(minQuantity.multiply(remainderUnit)) < 0) break;
        }

        if (totalRemainder.compareTo(BigDecimal.ZERO) > 0) {
            for (var component : components) {
                if (!component.isChanged()) {
                    var newLinePrice = component.getLinePrice().add(totalRemainder);
                    component.setLinePrice(newLinePrice);
                    component.setPrice(newLinePrice.divide(component.getQuantity(), currency.getDefaultFractionDigits(), RoundingMode.FLOOR));
                    component.setCanBeOdd(true);
                    return component;
                }
            }
        }
        var canBeOddComponent = components.get(0);
        canBeOddComponent.setCanBeOdd(true);
        return canBeOddComponent;
    }

    private CombinationCalculateResponse.LineItemComponent buildComboComponent(
            ComboItem comboItem,
            Product childProduct,
            BigDecimal originalComboPrice,
            BigDecimal lineItemQuantity,
            BigDecimal lineItemPrice,
            int zeroCount,
            BigDecimal appliedPrice,
            boolean isLastIndex,
            boolean isAvgSlit,
            Currency currency
    ) {
        var componentQuantity = comboItem.getQuantity().multiply(lineItemQuantity);
        var componentBuilder = CombinationCalculateResponse.LineItemComponent.builder()
                .variantId(comboItem.getVariantId())
                .sku(comboItem.getSku())
                .grams(comboItem.getGrams())
                .title(childProduct.getName())
                .taxable(comboItem.isTaxable())
                .variantTitle(comboItem.getTitle())
                .inventoryManagement(comboItem.getInventoryManagement())
                .inventoryPolicy(comboItem.getInventoryPolicy())
                .inventoryItemId(comboItem.getInventoryItemId() == null ? null : comboItem.getInventoryItemId().longValue())
                .vendor(childProduct.getVendor())
                .productId(comboItem.getProductId())
                .quantity(componentQuantity)
                .type(VariantType.combo)
                .baseQuantity(comboItem.getQuantity())
                .requireShipping(comboItem.isRequiresShipping())
                .unit(comboItem.getUnit());
        if (!isLastIndex) {
            // NOTE: Phân bổ giá về 1 thành phần
            var splitPrice = isAvgSlit || originalComboPrice.compareTo(BigDecimal.ZERO) <= 0
                    ? lineItemPrice.divide(BigDecimal.valueOf(zeroCount), currency.getDefaultFractionDigits(), RoundingMode.HALF_UP)
                    : comboItem.getPrice().multiply(comboItem.getQuantity()).multiply(lineItemPrice).divide(originalComboPrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
            // Giá của 1 quantity trong thành phần đó
            var unitComponentPrice = splitPrice.divide(componentQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            // Giá của line gốc
            var componentLinePrice = unitComponentPrice.multiply(componentQuantity);
            // Phần dư khi chia
            var remainder = splitPrice.subtract(componentLinePrice);

            return componentBuilder
                    .linePrice(componentLinePrice)
                    .subtotal(componentLinePrice)
                    .price(unitComponentPrice)
                    .remainder(remainder)
                    .build();
        }
        // Xử lý line cuối
        var splitPrice = lineItemPrice.subtract(appliedPrice);
        var unitComponentPrice = splitPrice.divide(componentQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
        var componentLinePrice = unitComponentPrice.multiply(componentQuantity);
        var remainder = splitPrice.subtract(componentLinePrice);
        return componentBuilder
                .linePrice(componentLinePrice)
                .subtotal(componentLinePrice)
                .price(unitComponentPrice)
                .remainder(remainder)
                .build();
    }

    private CalculateProductInfo getProductInfo(Integer storeId, CombinationCalculateRequest request) {
        var productInfo = CalculateProductInfo.builder();
        var baseVariantIds = request.getLineItems().stream()
                .filter(Objects::nonNull)
                .map(CombinationCalculateRequest.LineItem::getVariantId)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(baseVariantIds)) {
            return productInfo.build();
        }

        var baseVariants = sapoClient.productVariantFilter(storeId, baseVariantIds);
        // get product
        var allProductIds = baseVariants.stream()
                .map(ProductVariant::getProductId)
                .distinct()
                .collect(Collectors.toList());
        var variantMap = baseVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
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
            combos.stream().flatMap(com -> com.getComboItems().stream())
                    .forEach(item -> comboVariantMap.putIfAbsent(item.getVariantId(), combinationMapper.toResponse(item)));
            variantMap.putAll(comboVariantMap);
            allProductIds.addAll(
                    comboVariantMap.values().stream()
                            .map(ProductVariant::getProductId)
                            .filter(NumberUtils::isPositive)
                            .distinct()
                            .toList());
        }

        if (CollectionUtils.isNotEmpty(packsizeIds)) {
            var packsizes = sapoClient.packsizeFilterByVariantIds(storeId, packsizeIds);
            var packsizeVariantIds = packsizes.stream().map(Packsize::getVariantId).distinct().toList();
            var packsizeVariants = sapoClient.productVariantFilter(storeId, packsizeVariantIds).stream()
                    .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
            variantMap.putAll(packsizeVariants);
            productInfo.packsizeMap(packsizes.stream().collect(Collectors.toMap(Packsize::getPacksizeVariantId, Function.identity())));
            allProductIds.addAll(packsizes.stream().map(Packsize::getProductId).toList());
        }

        var productMap = sapoClient.productFilter(storeId, allProductIds)
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        var currency = Currency.getInstance(request.getCurrency());
        var remainderUnit = BigDecimal.ONE.movePointLeft(currency.getDefaultFractionDigits());
        if (request.isCalculateTax()) {
            Set<Integer> productIds = new HashSet<>(productMap.keySet());
            productIds.add(0);
            var taxSetting = taxHelper.getTaxSetting(storeId, request.getCountryCode(), productIds);
            var productTaxes = taxSetting.getTaxes().stream()
                    .filter(item -> NumberUtils.isPositive(item.getProductId()))
                    .collect(Collectors.toMap(TaxSettingValue::getProductId, Function.identity(), (first, second) -> second));
            var countryTax = taxSetting.getTaxes().stream()
                    .filter(item -> item.getProductId() == null)
                    .findFirst().orElse(TaxSettingValue.builder().rate(BigDecimal.ZERO).build());
            productInfo.productTaxMap(productTaxes)
                    .countryTaxSetting(countryTax);
        }

        return productInfo
                .currency(currency)
                .remainderUnit(remainderUnit)
                .variantMap(variantMap)
                .productMap(productMap)
                .build();
    }

    private void validateRequest(CombinationCalculateRequest request) {
        var errors = new ArrayList<UserError>();
        for (int i = 0; i < request.getLineItems().size(); i++) {
            var lineItem = request.getLineItems().get(i);
            if (!NumberUtils.isPositive(lineItem.getVariantId())
                    && (lineItem.getPrice() == null || lineItem.getPrice().signum() < 0)) {
                errors.add(UserError.builder()
                        .code("line_item[%s].price".formatted(i))
                        .message("price must be greater than or equal to 0")
                        .build());
            }
            if (!request.isUpdateProductInfo()
                    && (CollectionUtils.isNotEmpty(lineItem.getComponents()) && lineItem.getType() == VariantType.normal)) {
                errors.add(UserError.builder()
                        .code("line_item[%s].type".formatted(i))
                        .message("type must be combo or packsize")
                        .fields(List.of("line_item", String.valueOf(i), "type"))
                        .build());
            }
        }
        if (CollectionUtils.isNotEmpty(errors)) {
            var errorMessageBuilder = ErrorMessage.builder();
            for (var error : errors) {
                errorMessageBuilder.addError(error);
            }
            throw new ConstrainViolationException(errorMessageBuilder.build());
        }
    }
}