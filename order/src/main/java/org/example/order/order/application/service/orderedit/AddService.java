package org.example.order.order.application.service.orderedit;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.SapoClient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.model.order.context.Product;
import org.example.order.order.application.model.order.context.ProductVariant;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.orderedit.model.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class AddService {

    private final SapoClient sapoClient;

    /**
     * B1: check nếu không có event return <br/>
     * B2: tạo lineItem <br/>
     * B3: tạo lineItemDiscount <br/>
     * B4: commit vào order <br/>
     */
    public List<LineItem> addLineItems(
            Order order,
            OrderEdit orderEdit,
            OrderEditUtils.GroupedStagedChange changes
    ) {
        List<OrderStagedChange.AddVariant> addVariants = changes.addVariants();
        List<OrderStagedChange.AddCustomItem> addCustomItems = changes.addCustomItems();

        int lineItemCount = addVariants.size() + addCustomItems.size();
        if (lineItemCount == 0) {
            log.warn("Break for add new line item");
            return List.of();
        }

        var orderIdGenerator = order.getIdGenerator();
        var lineItemIds = orderIdGenerator.generateLineItemIds(lineItemCount);

        OrderEditId orderEditId = orderEdit.getId();
        List<Integer> variantIds = addVariants.stream()
                .map(OrderStagedChange.AddVariant::getVariantId)
                .filter(Objects::nonNull)
                .distinct().toList();
        ProductVariantInfo productVariantInfo = getProductVariants(orderEditId.getStoreId(), variantIds);

        // use for discount
        Map<UUID, LineItem> lineItemMap = new HashMap<>();
        // use for add line_items
        List<LineItem> lineItems = new ArrayList<>();

        Stream.concat(
                addVariants.stream()
                        .map(av -> buildAddVariantLineItem(av, productVariantInfo, lineItemIds.removeFirst())),
                addCustomItems.stream()
                        .map(aci -> buildAddCustomItem(aci, lineItemIds.removeFirst()))
        ).forEach(line -> {
            lineItemMap.put(line.lineItemId, line.lineItem);
            lineItems.add(line.lineItem);
        });

        return order.editAddNewLineItems(
                lineItems,
                getDiscountForNewLineItem(lineItemMap, changes, orderEdit)
        );
    }

    private Map<Integer, DiscountLineItem> getDiscountForNewLineItem(
            Map<UUID, LineItem> lineItemMap,
            OrderEditUtils.GroupedStagedChange changes,
            OrderEdit orderEdit
    ) {
        return lineItemMap.entrySet().stream()
                .map(entry -> getDiscountForLine(entry, changes, orderEdit))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        d -> d.lineItemId,
                        d -> d.discountLineItem));
    }

    private LineDiscountInfo getDiscountForLine(Map.Entry<UUID, LineItem> entry, OrderEditUtils.GroupedStagedChange changes, OrderEdit orderEdit) {
        List<OrderStagedChange.AddItemDiscount> addItemDiscounts = changes.addItemDiscounts();
        if (CollectionUtils.isEmpty(addItemDiscounts)) {
            return null;
        }
        UUID lineItemId = entry.getKey();
        var discountsOfLine = addItemDiscounts.stream()
                .filter(discount -> Objects.equals(discount.getLineItemId(), lineItemId))
                .toList();
        if (discountsOfLine.isEmpty())
            return null;
        if (discountsOfLine.size() >= 2)
            log.warn("Line item has more than 1 discount");

        Integer addedLineItemId = entry.getValue().getId();
        OrderStagedChange.AddItemDiscount addItemDiscount = discountsOfLine.get(0);

        UUID allocationId = addItemDiscount.getAllocationId();
        UUID applicationId = addItemDiscount.getApplicationId();

        AddedDiscountAllocation allocation = orderEdit.getDiscountAllocations().stream()
                .filter(discount -> Objects.equals(discount.getId(), allocationId))
                .findFirst()
                .orElse(null);
        AddedDiscountApplication application = orderEdit.getDiscountApplications().stream()
                .filter(discount -> Objects.equals(discount.getId(), applicationId))
                .findFirst()
                .orElse(null);

        if (allocation == null || application == null)
            throw new ConstrainViolationException("not found", "Require discount for existed lineItem has discount");
        DiscountLineItem discountLineItem = new DiscountLineItem(application, allocation);

        return new LineDiscountInfo(addedLineItemId, discountLineItem);
    }

    private record LineDiscountInfo(Integer lineItemId, DiscountLineItem discountLineItem) {
    }

    public record DiscountLineItem(AddedDiscountApplication application, AddedDiscountAllocation allocation) {

    }

    private ProductVariantInfo getProductVariants(int storeId, List<Integer> variantIds) {
        if (CollectionUtils.isEmpty(variantIds)) {
            return new ProductVariantInfo(Map.of(), Map.of());
        }

        var variants = sapoClient.productVariantFilter(storeId, variantIds).stream()
                .collect(Collectors.toMap(
                        ProductVariant::getId,
                        Function.identity()));
        var productIds = variants.values().stream()
                .map(ProductVariant::getProductId)
                .filter(NumberUtils::isPositive)
                .distinct().toList();
        var products = sapoClient.productFilter(storeId, productIds).stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Function.identity()));

        return new ProductVariantInfo(variants, products);
    }

    private record ProductVariantInfo(Map<Integer, ProductVariant> variantMap, Map<Integer, Product> productMap) {
    }

    private record LineItemCreated(UUID lineItemId, LineItem lineItem) {
    }

    private LineItemCreated buildAddCustomItem(OrderStagedChange.AddCustomItem aci, Integer lineItemId) {
        return new LineItemCreated(
                aci.getLineItemId(),
                new LineItem(
                        lineItemId,
                        aci.getQuantity().intValue(),
                        aci.getPrice(),
                        BigDecimal.ZERO,
                        null,
                        getVariantInfo(aci),
                        aci.isTaxable(),
                        new ArrayList<>(),
                        "service",
                        false,
                        null
                )
        );
    }

    private VariantInfo getVariantInfo(OrderStagedChange.AddCustomItem aci) {
        return VariantInfo.builder()
                .title(aci.getTitle())
                .productExisted(false)
                .build();
    }

    private LineItemCreated buildAddVariantLineItem(OrderStagedChange.AddVariant av, ProductVariantInfo productVariantInfo, Integer lineItemId) {
        Map<Integer, ProductVariant> variantMap = productVariantInfo.variantMap;
        Map<Integer, Product> productMap = productVariantInfo.productMap;

        assert av.getVariantId() != null;

        Integer variantId = av.getVariantId();
        var variant = variantMap.get(variantId);
        if (variant == null) {
            throw new ConstrainViolationException("variant", "not found");
        }
        var product = productMap.get(variant.getProductId());

        return new LineItemCreated(
                av.getLineItemId(),
                new LineItem(
                        lineItemId,
                        av.getQuantity().intValue(),
                        variant.getPrice(),
                        BigDecimal.ZERO,
                        null,
                        getVariantInfo(variant, product),
                        variant.isTaxable(),
                        new ArrayList<>(),
                        "service",
                        false,
                        null
                )
        );
    }

    private VariantInfo getVariantInfo(ProductVariant variant, Product product) {
        return VariantInfo.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .productExisted(true)
                .name(variant.getTitle())
                .title(product.getName())
                .variantTitle(variant.getTitle())
                .vendor(product.getVendor())
                .sku(variant.getSku())
                .grams(variant.getGrams())
                .requireShipping(variant.isRequiresShipping())
                .variantInventoryManagement("manage")
                .restockable(false)
                .inventoryItemId(variant.getInventoryItemId().longValue())
                .unit(variant.getUnit())
                .build();
    }
}
