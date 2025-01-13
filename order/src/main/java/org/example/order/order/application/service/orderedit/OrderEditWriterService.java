package org.example.order.order.application.service.orderedit;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.orderedit.model.AddedLineItem;
import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.example.order.order.domain.orderedit.persistence.OrderEditRepository;
import org.example.order.order.infrastructure.data.dto.Location;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEditWriterService {

    private final AbstractOrderEditProcessor orderEditProcessor;

    private final EditContextService contextService;

    private final OrderEditRepository orderEditRepository;

    @Transactional
    public OrderEditId beginEdit(int storeId, int id) {
        return orderEditProcessor.beginEdit(storeId, id);
    }

    /**
     * Get context tương ứng với từng request => thuận tiện trong việc tính toán sau này
     */
    @Transactional
    public List<UUID> addVariants(OrderEditId orderEditId, OrderEditRequest.AddVariants addVariants) {
        var context = contextService.createContext(orderEditId, addVariants.getVariants());
        List<UUID> addedLineItemIds = addVariants.getVariants().stream()
                .map(addVariant -> addVariant(addVariant, context))
                .toList();
        orderEditRepository.save(context.orderEdit());
        return addedLineItemIds;
    }

    private UUID addVariant(OrderEditRequest.AddVariant request, EditContextService.AddVariantsContext context) {
        Location location = context.getLocation(Optional.ofNullable(request.getLocationId()).map(Long::valueOf).orElse(null));

        var variantInfo = context.getVariantInfo(request.getVariantId());
        var variant = variantInfo.variant();
        var product = variantInfo.product();

        final OrderEdit editing = context.orderEdit();

        if (!request.isAllowDuplicate()) {
            final int variantId = request.getVariantId();
            boolean duplicateAdded = editing.getLineItems().stream()
                    .anyMatch(line -> Objects.equals(line.getVariantId(), variantId));

            Order order = context.order();
            assert order != null : "Order should be fetched if request requires checking duplicates";
            boolean duplicateOrderLine = order.getLineItems().stream()
                    .anyMatch(line -> Objects.equals(line.getVariantInfo().getVariantId(), variantId));

            if (duplicateAdded || duplicateOrderLine) {
                throw new ConstrainViolationException(UserError.builder()
                        .message("line item with variant id duplicate")
                        .build());
            }
        }

        var lineItem = createAddVariantLineItem(variant, product, location, request);
        editing.addLineItem(lineItem, context);
        return lineItem.getId();
    }

    private AddedLineItem createAddVariantLineItem(VariantDto variant, ProductDto product, Location location, OrderEditRequest.AddVariant request) {
        return new AddedLineItem(
                BigDecimal.valueOf(request.getQuantity()),
                variant.getId(),
                product.getId(),
                (int) location.getId(),
                variant.getSku(),
                product.getName(),
                variant.getTitle(),
                variant.getPrice(),
                variant.isTaxable(),
                variant.isRequiresShipping(),
                false
        );
    }

    @Transactional
    public UUID addCustomItem(OrderEditId orderEditId, OrderEditRequest.AddCustomItem request) {
        var context = contextService.createContext(orderEditId, request);

        var newLineItem = buildCustomItem(context.location(), request);

        final OrderEdit editing = context.orderEdit();
        editing.addLineItem(newLineItem, context);

        orderEditRepository.save(editing);

        return newLineItem.getId();
    }

    private AddedLineItem buildCustomItem(Location location, OrderEditRequest.AddCustomItem request) {
        return new AddedLineItem(
                BigDecimal.valueOf(request.getQuantity()),
                null,
                null,
                (int) location.getId(),
                null,
                request.getTitle(),
                null,
                request.getPrice(),
                request.isTaxable(),
                request.isRequireShipping(),
                false
        );
    }

    @Transactional
    public String updateQuantity(OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        var context = contextService.createContext(orderEditId, request);
        OrderEdit orderEdit = context.orderEdit();

        if (context instanceof EditContextService.RemovedItemContext removeContext) {

            orderEdit.removeItem(removeContext.lineItem().getId(), removeContext.taxSetting());

        } else if (context instanceof EditContextService.AdjustAddedItemContext adjustQuantity) {
            orderEdit.updateAddedLineItemQuantity(
                    adjustQuantity.lineItem().getId(),
                    adjustQuantity.taxSetting(),
                    request.getQuantity()
            );
        } else if (context instanceof EditContextService.ExistingItemContext existingItemContext) {
            LineItem lineItem = existingItemContext.lineItem();
            boolean fulfilled = lineItem.getFulfillableQuantity() == 0;
            if (fulfilled) {
                throw new ConstrainViolationException("line_item", "");
            }

            boolean hasDiscount = CollectionUtils.isNotEmpty(lineItem.getDiscountAllocations());
            if (hasDiscount && request.getQuantity() != lineItem.getFulfillableQuantity()) {
                throw new ConstrainViolationException("line_item", "");
            }

            orderEdit.recordQuantityAdjustment(
                    lineItem,
                    request.getQuantity(),
                    request.isRestock(),
                    existingItemContext
            );
        }

        orderEditRepository.save(orderEdit);
        return request.getLineItemId();
    }

    @Transactional
    public void applyDiscount(OrderEditId orderEditId, OrderEditRequest.SetItemDiscount request) {
        var context = contextService.createContext(orderEditId, request);

        var discountRequest = context.discountRequest();
        var editing = context.orderEdit();

        UUID lineItemId = context.lineItem().getId();

        if (discountRequest.amount().signum() == 0) {
            editing.removeDiscount(context.lineItem());
            return;
        }

        editing.applyDiscount(context.lineItem(), discountRequest);
        orderEditRepository.save(editing);
    }
}
