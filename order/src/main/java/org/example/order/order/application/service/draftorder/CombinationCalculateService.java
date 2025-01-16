package org.example.order.order.application.service.draftorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
        if (request.isUpdateProductInfo()) {
            result = calculate(request, productInfo);
        } else {
            result = calculateWithoutUpdateProductInfo(request, productInfo);
        }
        return result;
    }

    private CombinationCalculateResponse calculate(CombinationCalculateRequest request, CalculateProductInfo productInfo) {
        var variantMap = productInfo.getVariantMap();
        var productMap = productInfo.getProductMap();
        var packsizeMap = productInfo.getPacksizeMap();
        var currency = productInfo.getCurrency();

        List<CombinationCalculateResponse.LineItem> lineResponses = new ArrayList<>();
        for (var lineItemRequest : request.getLineItems()) {
            BigDecimal lineItemQuantity = lineItemRequest.getQuantity();
            BigDecimal lineItemPrice; // Giá của line gốc

            if (lineItemRequest.getVariantId() == null) {
                var customLineItem = combinationMapper.toResponse(lineItemRequest);
                lineItemPrice = customLineItem.getPrice().multiply(lineItemQuantity);
                customLineItem.setLinePrice(lineItemPrice);
                lineResponses.add(customLineItem);
                continue;
            }

            var variant = variantMap.get(lineItemRequest.getVariantId());
            if (variant == null) {
                throw new ConstrainViolationException(UserError.builder()
                        .fields(List.of("variant_id"))
                        .build());
            }
            var product = productMap.get(lineItemRequest.getProductId());
            var lineItem = combinationMapper.toResponse(lineItemRequest, variant, product);
            lineResponses.add(lineItem);

            //Note: Nếu truyền cả variantId và price thì ưu tiên lấy price hơn
            if (lineItemRequest.getPrice() != null && lineItemRequest.getPrice().compareTo(BigDecimal.ZERO) >= 0) {
                lineItem.setPrice(lineItemRequest.getPrice());
                lineItemPrice = lineItemRequest.getPrice().multiply(lineItemQuantity);
            } else {
                lineItemPrice = variant.getPrice().multiply(lineItemQuantity);
            }
            lineItem.setLinePrice(lineItemPrice);

            // tuỳ theo loại sản phẩm sẽ phân bổ giá khác nhau
            switch (lineItemRequest.getType()) {
                case packsize -> {
                    var packsize = packsizeMap.get(lineItemRequest.getVariantId());
                    var childProduct = productMap.get(packsize.getProductId());
                    var childVariant = variantMap.get(packsize.getVariantId());
                    if (childVariant == null) { //Có trường hợp nào null không ??
                        lineItem.setComponents(List.of());
                        continue;
                    }

                    var baseQuantity = packsize.getQuantity();
                    var quantity = baseQuantity.multiply(lineItemQuantity);
                    // Giá của 1 quantity trong packsize
                    var itemUnitPrice = lineItemPrice.divide(quantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR); // Làm tròn xuống
                    // Phần dư
                    var remainder = lineItemPrice.subtract(itemUnitPrice.multiply(quantity));
                    var component = combinationMapper.toResponse(
                            childVariant,
                            childProduct,
                            quantity, // Tổng quantity trong đơn hàng
                            baseQuantity, // Quantity trong 1 packsize
                            remainder, // Phần dư khi chia
                            itemUnitPrice, // Giá của 1 quantity
                            lineItemPrice, // Tổng giá của lineItem
                            lineItemRequest.getDiscountAllocations(),
                            VariantType.packsize
                    );
                    component.setBaseQuantity(baseQuantity);
                    if (NumberUtils.isPositive(remainder)) component.setCanBeOdd(true);
                    lineItem.setComponents(List.of(component));
                }
                case combo -> {
                    var components = buildComponents(lineItemQuantity, lineItemPrice, variant, productInfo, lineItemRequest.getDiscountAllocations());
                    lineItem.setComponents(components);
                }
                case normal -> {
                    //Note: Giữ nguyên như cũ
                }
            }
        }
        if (request.isCalculateTax()) {
            var countryTax = productInfo.getCountryTaxSetting();
            for (var lineItem : lineResponses) {
                handleTaxLines(lineItem, countryTax, currency, request.isTaxIncluded(), request.isTaxExempt(), productInfo);
            }
        }
        return CombinationCalculateResponse.builder()
                .lineItems(lineResponses)
                .build();
    }

    private void handleTaxLines(
            CombinationCalculateResponse.LineItem lineItem,
            TaxSettingValue countryTax,
            Currency currency,
            boolean taxIncluded,
            boolean taxExempt,
            CalculateProductInfo productInfo
    ) {
        Map<MergedTaxLine.TaxLineKey, MergedTaxLine> taxLineMap = new HashMap<>();
        var isCustomTax = CollectionUtils.isNotEmpty(lineItem.getTaxLines());
        if (isCustomTax) {
            taxLineMap = lineItem.getTaxLines().stream()
                    .collect(MergedTaxLine.merge()); // set lại taxLine sao không trùng key
        }
        switch (lineItem.getType()) {
            case combo, packsize -> {
                var components = lineItem.getComponents();
                if (isCustomTax) { // Nếu có taxLine từ request
                    int componentCount = components.size();
                    var taxLines = taxLineMap.values().stream().toList();
                    for (var taxLine : taxLines) {
                        var appliedAmount = BigDecimal.ZERO;
                        for (int i = 0; i < componentCount; i++) {
                            var component = components.get(i);
                            BigDecimal price;
                            if (i != componentCount - 1) {
                                price = MergedTaxLine.distribute(taxLine, component.getLinePrice(), lineItem.getLinePrice(), currency);
                                appliedAmount = appliedAmount.add(price);
                                // set lại taxLine cho line thành phần
                            } else {
                                price = taxLine.getPrice().subtract(appliedAmount);
                            }
                            var componentTaxLine = new CombinationCalculateResponse.ComboPacksizeTaxLine(taxLine).toBuilder()
                                    .price(price)
                                    .build();
                            component.addTaxLine(componentTaxLine);
                        }
                    }
                } else if (!taxExempt) {
                    for (var component : components) {
                        if (!component.isTaxable()) continue;
                        var productTax = productInfo.getProductTaxMap().get(component.getProductId());
                        var taxLine = MergedTaxLine.buildComboPacksizeTaxLine(productTax, countryTax, component.getSubtotal(), taxIncluded, currency);
                        component.setTaxLines(List.of(taxLine));
                        MergedTaxLine.merge(taxLineMap, taxLine);
                    }
                }

                var finalTaxLines = MergedTaxLine.toValue(taxLineMap).values().stream().toList();
                lineItem.setTaxLines(finalTaxLines);
            }
            case normal -> {
                // NOTE: Giữ nguyên như cũ
            }
        }
    }

    private List<CombinationCalculateResponse.LineItemComponent> buildComponents(
            BigDecimal lineItemQuantity,
            BigDecimal lineItemPrice,
            ProductVariant variant,
            CalculateProductInfo productInfo,
            List<CombinationCalculateResponse.ComboPacksizeDiscountAllocation> discountAllocations) {
        var combo = productInfo.getComboMap().get(variant.getId());
        if (combo == null) {
            throw new ConstrainViolationException(UserError.builder()
                    .fields(List.of("variant_id", "combo"))
                    .build());
        }
        var comboItems = combo.getComboItems();
        if (CollectionUtils.isEmpty(comboItems)) return List.of();
        var originalComboPrice = comboItems.stream()
                .map(item -> item.getPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        /**
         * Khi chia và phân bổ giá thì có 2 trường hợp
         * + Chia cho các combo-item có price != 0
         * + Chia cho các combo-item có price == 0
         * */
        if (comboItems.stream().anyMatch(item -> item.getPrice() == null)) {
            log.debug("combos with id {} has price item is null", variant.getId());
        }
        // các combo-item có price > 0
        var positivePriceLineItems = comboItems.stream().filter(item -> NumberUtils.isPositive(item.getPrice())).toList();
        // các combo-item có price = 0
        var zeroPriceLineItems = comboItems.stream().filter(item -> !NumberUtils.isPositive(item.getPrice())).toList();

        int positiveCount = positivePriceLineItems.size();
        int zeroCount = zeroPriceLineItems.size();
        //NOTE: Tổng giá đã phân bổ về item
        List<CombinationCalculateResponse.LineItemComponent> components = new ArrayList<>();
        BigDecimal totalRemainder = BigDecimal.ZERO;
        BigDecimal allocatedComboPrice = BigDecimal.ZERO;
        BigDecimal minQuantity = positivePriceLineItems.get(0).getQuantity();
        for (int i = 0; i < positiveCount; i++) {
            var comboItem = positivePriceLineItems.get(i);
            var childProduct = productInfo.getProductMap().get(comboItem.getProductId());
            var component = buildComponent(
                    comboItem,
                    childProduct,
                    originalComboPrice,
                    lineItemPrice,
                    zeroCount,
                    allocatedComboPrice,
                    i == positiveCount - 1,
                    false,
                    productInfo.getCurrency()
            );
            components.add(component);
            allocatedComboPrice = allocatedComboPrice.add(component.getLinePrice()).add(component.getRemainder());
            totalRemainder = totalRemainder.add(component.getRemainder());
            if (component.getQuantity().compareTo(minQuantity) < 0) minQuantity = component.getQuantity();
        }
        for (int i = 0; i < zeroCount; i++) {
            var comboItem = zeroPriceLineItems.get(i);
            var childProduct = productInfo.getProductMap().get(comboItem.getProductId());
            var component = buildComponent(
                    comboItem,
                    childProduct,
                    originalComboPrice,
                    lineItemPrice,
                    zeroCount,
                    allocatedComboPrice,
                    i == zeroCount - 1,
                    zeroCount == comboItems.size(),
                    productInfo.getCurrency()
            );
            components.add(component);
            allocatedComboPrice = allocatedComboPrice.add(component.getLinePrice()).add(component.getRemainder());
            totalRemainder = totalRemainder.add(component.getRemainder());
            if (component.getQuantity().compareTo(minQuantity) < 0) minQuantity = component.getQuantity();
        }

        // Xử lý phần dư theo thứ tự giảm dần, xử lý các item có phần dư từ lớn đến nhỏ
        var sortedComponents = components.stream()
                .sorted(Comparator.comparing((CombinationCalculateResponse.LineItemComponent::getRemainder)).reversed())
                .toList();
        var canBeOddComponent = addRemainder(totalRemainder, minQuantity, productInfo.getRemainderUnit(), sortedComponents, productInfo.getCurrency());
        handleDiscountAllocations(canBeOddComponent, components, sortedComponents, discountAllocations, lineItemPrice, minQuantity, productInfo);
        return components;
    }

    private void handleDiscountAllocations(
            CombinationCalculateResponse.LineItemComponent canBeOddComponent,
            List<CombinationCalculateResponse.LineItemComponent> components,
            List<CombinationCalculateResponse.LineItemComponent> sortedComponents,
            List<CombinationCalculateResponse.ComboPacksizeDiscountAllocation> discountAllocations,
            BigDecimal lineItemPrice,
            BigDecimal minQuantity,
            CalculateProductInfo productInfo) {
        if (CollectionUtils.isEmpty(discountAllocations)) return;
        var currency = productInfo.getCurrency();
        var componentCount = components.size();
        int lastLineHasPriceIndex = componentCount - 1; // default ??
        for (int i = componentCount - 1; i >= 0; i--) {
            var component = components.get(i);
            if (NumberUtils.isPositive(component.getPrice())) {
                lastLineHasPriceIndex = i;
                break;
            }
        }
        for (var discount : discountAllocations) {
            var allocatedAmount = BigDecimal.ZERO;
            var totalRemainder = BigDecimal.ZERO;
            for (int i = 0; i < componentCount - 1; i++) {
                var component = components.get(i);
                // Giá được phân bổ về line
                var splitPrice = i == componentCount - 1
                        ? discount.getAmount().subtract(allocatedAmount)
                        : discount.getAmount().multiply(component.getLinePrice()).divide(lineItemPrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
                allocatedAmount = allocatedAmount.add(splitPrice);
                var amount = splitPrice.min(component.getSubtotal())
                        .divide(component.getQuantity(), currency.getDefaultFractionDigits(), RoundingMode.FLOOR)
                        .multiply(component.getQuantity());
                // Phần dư sau khi chia cho từng line
                var remainder = splitPrice.subtract(amount);
                totalRemainder = totalRemainder.add(remainder);
                var allocation = discount.toBuilder()
                        .amount(amount)
                        .remainder(remainder)
                        .build();
                component.addDiscountAllocation(allocation);
                // ?? Nếu có nhiều dícount phân bổ về component này thì sẽ sai(replace allocation trước đó)
                component.setRemainderDiscountAllocation(allocation);
            }
            // Sắp xếp theo chiều giảm dần của remainder trong remainderDiscount
            var storedComponentByDiscountRemainder = components.stream()
                    .sorted(Comparator.comparing(
                            (CombinationCalculateResponse.LineItemComponent component)
                                    -> component.getRemainderDiscountAllocation().getRemainder()).reversed())
                    .toList();
            reallocateAmountToComponents(canBeOddComponent, storedComponentByDiscountRemainder, sortedComponents, totalRemainder, minQuantity, productInfo);
        }
    }

    private void reallocateAmountToComponents(
            CombinationCalculateResponse.LineItemComponent canBeOddComponent,
            List<CombinationCalculateResponse.LineItemComponent> storedComponentByDiscountRemainder,
            List<CombinationCalculateResponse.LineItemComponent> sortedComponents,
            BigDecimal totalRemainder,
            BigDecimal minQuantity,
            CalculateProductInfo productInfo
    ) {
        if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0) return;

        //NOTE: Quy tắc: thêm vào 1 lượng = remainderUnit * quantity
        // phân bổ lại lần lượt cho component có remainder lớn -> nhỏ
        var remainderUnit = productInfo.getRemainderUnit();
        for (var component : storedComponentByDiscountRemainder) {
            var discountAllocation = component.getRemainderDiscountAllocation();
            var addPrice = component.getQuantity().multiply(remainderUnit);
            if (addPrice.compareTo(totalRemainder) <= 0) {
                var newSubtotal = component.getSubtotal().subtract(addPrice);
                discountAllocation.addAmount(addPrice);
                totalRemainder = totalRemainder.subtract(addPrice);
                component.setSubtotal(newSubtotal);
            }
            if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0)
                return;
            if (totalRemainder.compareTo(minQuantity.multiply(remainderUnit)) <= 0) break;
        }

        var oddComponentDiscount = canBeOddComponent.getRemainderDiscountAllocation();
        var aPrice = totalRemainder.min(canBeOddComponent.getSubtotal());
        oddComponentDiscount.addAmount(aPrice);
        totalRemainder = totalRemainder.subtract(aPrice);
        canBeOddComponent.setSubtotal(canBeOddComponent.getSubtotal().subtract(aPrice));

        if (totalRemainder.compareTo(BigDecimal.ZERO) > 0) {
            for (var component : sortedComponents) {
                var discountAllocation = component.getRemainderDiscountAllocation();
                var addPrice = component.getSubtotal().min(totalRemainder);
                var newSubtotal = component.getSubtotal().subtract(addPrice);
                discountAllocation.addAmount(addPrice);
                component.setSubtotal(newSubtotal);
                totalRemainder = totalRemainder.subtract(addPrice);
                if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0)
                    return;
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
            var addPrice = component.getQuantity().multiply(remainderUnit); // áp theo 1 qui tắc nào đó
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

    private CombinationCalculateResponse.LineItemComponent buildComponent(
            ComboItem comboItem,
            Product childProduct,
            BigDecimal originalComboPrice,
            BigDecimal lineItemPrice,
            int zeroCount,
            BigDecimal allocatedComboPrice,
            boolean isLastLine,
            boolean isAvgSplit,
            Currency currency
    ) {
        var comboItemQuantity = comboItem.getQuantity();
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
                .productId(childProduct.getId())
                .type(VariantType.combo)
                .baseQuantity(comboItemQuantity)
                .requireShipping(comboItem.isRequiresShipping())
                .unit(comboItem.getUnit());
        if (!isLastLine) {
            var splitPrice = isAvgSplit || originalComboPrice.compareTo(BigDecimal.ZERO) == 0
                    ? lineItemPrice.divide(BigDecimal.valueOf(zeroCount), currency.getDefaultFractionDigits(), RoundingMode.HALF_UP)
                    : comboItem.getPrice().multiply(comboItem.getQuantity()).multiply(lineItemPrice).divide(originalComboPrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
            // Giá của 1 quantity
            var unitPrice = splitPrice.divide(comboItem.getQuantity(), currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            // Tổng giá của line thành phần
            var componentLinePrice = unitPrice.multiply(comboItem.getQuantity());
            // phần dư khi chia
            var remainder = splitPrice.subtract(componentLinePrice);
            return componentBuilder
                    .linePrice(componentLinePrice)
                    .subtotal(componentLinePrice)
                    .price(unitPrice)
                    .remainder(remainder)
                    .build();
        }

        //Nếu là line cuối
        var splitPrice = lineItemPrice.subtract(allocatedComboPrice);
        var unitPrice = splitPrice.divide(comboItemQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
        var componentLinePrice = unitPrice.multiply(comboItemQuantity);
        var remainder = splitPrice.subtract(componentLinePrice);
        return componentBuilder
                .price(unitPrice)
                .subtotal(componentLinePrice)
                .linePrice(componentLinePrice)
                .remainder(remainder)
                .build();
    }

    /**
     * Note: Thông tin sản phẩm thì lấy theo input
     */
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
                var canBeOddComponent = components.stream().filter(CombinationCalculateResponse.LineItemComponent::isChanged).findFirst().orElse(components.get(0));
                var sortedComponents = components.stream().sorted(Comparator.comparing(CombinationCalculateResponse.LineItemComponent::getRemainder)).toList();
                handleDiscountAllocations(canBeOddComponent, components, sortedComponents, lineItem.getDiscountAllocations(), lineItemPrice, minQuantity, productInfo);
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

    private CalculateProductInfo getProductInfo(Integer storeId, CombinationCalculateRequest request) {
        var productInfo = CalculateProductInfo.builder();
        var baseVariantIds = request.getLineItems().stream()
                .map(CombinationCalculateRequest.LineItem::getVariantId)
                .filter(NumberUtils::isPositive)
                .distinct().toList();
        if (CollectionUtils.isEmpty(baseVariantIds)) return productInfo.build();

        var baseVariants = sapoClient.productVariantFilter(storeId, baseVariantIds);
        var variantMap = baseVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        var allProductIds = baseVariants.stream()
                .map(ProductVariant::getProductId)
                .distinct()
                .collect(Collectors.toList());

        var comboVariantIds = baseVariants.stream()
                .filter(variant -> variant.getType() == VariantType.combo)
                .map(ProductVariant::getId)
                .toList();
        var packsizeIds = baseVariants.stream()
                .filter(variant -> variant.getType() == VariantType.packsize)
                .map(ProductVariant::getId)
                .toList();
        if (CollectionUtils.isNotEmpty(comboVariantIds)) {
            var combos = sapoClient.comboFilter(storeId, comboVariantIds);
            productInfo.comboMap(combos.stream().collect(Collectors.toMap(Combo::getVariantId, Function.identity())));

            combos.stream()
                    .flatMap(combo -> combo.getComboItems().stream())
                    .forEach(item -> {
                        allProductIds.add(item.getProductId());
                        variantMap.putIfAbsent(item.getVariantId(), combinationMapper.toResponse(item));
                    });
        }
        if (CollectionUtils.isNotEmpty(packsizeIds)) {
            var packsizes = sapoClient.packsizeFilterByVariantIds(storeId, packsizeIds);
            productInfo.packsizeMap(packsizes.stream().collect(Collectors.toMap(Packsize::getVariantId, Function.identity())));
            var packsizeVariantIds = packsizes.stream()
                    .map(Packsize::getPacksizeVariantId)
                    .distinct().toList();
            var packsizeVariantMap = sapoClient.productVariantFilter(storeId, packsizeVariantIds).stream()
                    .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
            variantMap.putAll(packsizeVariantMap);
            allProductIds.addAll(packsizeVariantMap.values().stream().map(ProductVariant::getProductId).distinct().toList());
        }

        var productMap = sapoClient.productFilter(storeId, allProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        var currency = StringUtils.isNotBlank(request.getCurrency()) ? Currency.getInstance(request.getCurrency()) : Currency.getInstance("VND");
        var remainderUnit = BigDecimal.ONE.movePointLeft(2);

        if (request.isCalculateTax()) {
            Set<Integer> productIds = productMap.keySet();
            productIds.add(0);
            var taxSetting = taxHelper.getTaxSetting(storeId, request.getCountryCode(), productIds);
            var countryTax = taxSetting.getTaxes().stream()
                    .filter(tax -> !NumberUtils.isPositive(tax.getProductId()))
                    .findFirst()
                    .orElse(TaxSettingValue.builder().rate(BigDecimal.ZERO).build());

            var taxMap = taxSetting.getTaxes().stream()
                    .collect(Collectors.toMap(
                            TaxSettingValue::getProductId,
                            Function.identity(),
                            (first, second) -> second));
            productInfo
                    .productTaxMap(taxMap)
                    .countryTaxSetting(countryTax);
        }

        return productInfo
                .productMap(productMap)
                .variantMap(variantMap)
                .currency(currency)
                .remainderUnit(remainderUnit)
                .build();
    }

    private void validateRequest(CombinationCalculateRequest request) {
        List<UserError> userErrors = new ArrayList<>();
        for (int i = 0; i < request.getLineItems().size(); i++) {
            var lineItem = request.getLineItems().get(i);
            if (!NumberUtils.isPositive(lineItem.getVariantId())
                    && (lineItem.getPrice() == null || lineItem.getPrice().signum() < 0)) {
                userErrors.add(UserError.builder()
                        .code("line_items[%s].price".formatted(i))
                        .message("price must be greater than or equal to 0")
                        .fields(List.of("line_item", String.valueOf(lineItem.getPrice()), "price"))
                        .build());
            }
            if (!request.isUpdateProductInfo()
                    && (CollectionUtils.isNotEmpty(lineItem.getComponents()) && lineItem.getType() == VariantType.normal)) {
                userErrors.add(UserError.builder()
                        .code("line_items[%s].type".formatted(i))
                        .message("type must be combo or packsize")
                        .fields(List.of("line_item", String.valueOf(i), "type"))
                        .build());
            }
        }
        if (CollectionUtils.isNotEmpty(userErrors)) {
            var errorMessageBuilder = ErrorMessage.builder();
            for (var error : userErrors) {
                errorMessageBuilder.addError(error);
            }
            throw new ConstrainViolationException(errorMessageBuilder.build());
        }
    }
}