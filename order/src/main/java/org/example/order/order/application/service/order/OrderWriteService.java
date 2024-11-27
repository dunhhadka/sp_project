package org.example.order.order.application.service.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.order.context.OrderCreatedEvent;
import org.example.order.order.application.model.order.context.OrderCustomerContext;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.request.OrderRoutingItemRequest;
import org.example.order.order.application.model.order.request.RefundRequest;
import org.example.order.order.application.model.order.request.TransactionCreateRequest;
import org.example.order.order.application.model.order.response.OrderRoutingResponse;
import org.example.order.order.application.model.order.response.RefundCalculateResponse;
import org.example.order.order.application.service.customer.CustomerService;
import org.example.order.order.application.utils.*;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.model.TransactionRepository;
import org.example.order.order.infrastructure.data.dao.LocationDao;
import org.example.order.order.infrastructure.data.dao.OrderDao;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dao.StoreDao;
import org.example.order.order.infrastructure.data.dto.Location;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.StoreDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWriteService {

    private final OrderIdGenerator orderIdGenerator;

    private final StoreDao storeDao;
    private final OrderDao orderDao;
    private final ProductDao productDao;
    private final LocationDao locationDao;

    private final OrderMapper orderMapper;

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;

    private final CustomerService customerService;
    private final RefundCalculationService calculationService;

    private final MessageSource messageSource;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderId create(Integer storeId, OrderCreateRequest orderRequest) {
        Objects.requireNonNull(storeId);

        var store = getStoreById(storeId);

        var currency = resolveCurrency(orderRequest.getCurrency(), store.getCurrency());

        var sourceInfo = mapSource(orderRequest.getClientId(), orderRequest.getSource(), orderRequest.getSourceName());
        var reference = buildReference(storeId, orderRequest);
        var trackingInfo = buildTrackingInfo(sourceInfo.getLeft(), sourceInfo.getRight(), orderRequest, reference);

        validateTaxLineRequest(orderRequest, currency);

        var customerCtx = resolveCustomer(storeId, orderRequest);

        var customerInfo = new CustomerInfo(
                StringUtils.firstNonBlank(
                        orderRequest.getEmail(),
                        customerCtx.getEmail()),
                StringUtils.firstNonBlank(
                        orderRequest.getPhone(),
                        customerCtx.getPhone()),
                customerCtx.getCustomerId(),
                customerCtx.isAcceptsMarketing());

        var address = buildAddress(orderRequest.getBillingAddress(), orderRequest.getBillingAddress());

        var combinationLines = buildCombinationLines(orderRequest);

        var lineItems = buildLineItems(storeId, orderRequest.getLineItems(), combinationLines, currency);

        var shippingLines = buildShippingLines(storeId, orderRequest.getShippingLines());

        var discountCodes = buildOrderDiscountCodes(
                orderRequest.getDiscountCodes(),
                orderRequest.getTotalDiscount(),
                lineItems,
                shippingLines,
                currency);

        boolean isFromTrustedSource = orderRequest.getClientId() != null;

        var appliedDiscountInfo = this.allocateDiscounts(
                isFromTrustedSource,
                discountCodes,
                currency,
                lineItems,
                shippingLines,
                orderRequest);

        handleTaxLinesOrderRequest(orderRequest, lineItems, appliedDiscountInfo.getLeft(), currency);

        var location = validateLocation(storeId, orderRequest);

        Instant processOn = Instant.now();

        var order = new Order(
                storeId,
                processOn,
                customerInfo,
                trackingInfo,
                currency,
                orderRequest.getTotalWeight(),
                orderRequest.getNote(),
                orderRequest.getTags(),
                address.getLeft(),
                address.getRight(),
                lineItems,
                shippingLines,
                discountCodes,
                appliedDiscountInfo.getLeft(),
                appliedDiscountInfo.getRight(),
                orderRequest.isTaxExempt(),
                orderRequest.isTaxesIncluded(),
                orderRequest.getGateWay(),
                orderRequest.getProcessingMethod(),
                this.orderIdGenerator,
                location == null ? null : location.getId(),
                combinationLines
        );

        if (!CollectionUtils.isEmpty(orderRequest.getFufillments())) {
            order.markAsFulfilled();
        }

        var paymentResult = prepareOrderPayment(order, orderRequest);
        if (!CollectionUtils.isEmpty(paymentResult.getTransactions())) {
            var transactions = paymentResult.getTransactions();
            transactions.forEach(order::recognizeTransaction);
        }

        var orderRoutingResponse = processOrderRouting(order, location);

        orderRepository.save(order);

        var orderCreated = new OrderCreatedEvent(
                store,
                order.getId(),
                orderRoutingResponse,
                orderRequest.getFufillments(),
                paymentResult,
                orderRequest.getTransactions()
        );
        eventPublisher.publishEvent(orderCreated);

        return order.getId();
    }

    private OrderPaymentResult prepareOrderPayment(Order order, OrderCreateRequest request) {
        List<Order.TransactionInput> transactionInputs = new ArrayList<>();
        var checkoutToken = request.getCheckoutToken();
        var transactionsRequest = request.getTransactions();
        var storeId = order.getId().getStoreId();
        var isFromCheckout = false; // Đơn từ online
        List<Integer> paymentIds = new ArrayList<>();

        // Kiểm tra payment và transactions của checkount
        if (StringUtils.isNotBlank(checkoutToken)) {
            var payments = new ArrayList<>(); // get payments từ db;
            if (!CollectionUtils.isEmpty(payments)) {
                paymentIds = new ArrayList<Integer>();
                var checkoutTransactions = new ArrayList<>();
                if (!CollectionUtils.isEmpty(checkoutTransactions)) {
                    transactionInputs = new ArrayList<>(); // build transactionInputs
                }
            }
            if (!CollectionUtils.isEmpty(transactionInputs)) isFromCheckout = true;
        }

        if (!isFromCheckout && !CollectionUtils.isEmpty(transactionsRequest)) {
            preCheckTransactionsInput(transactionsRequest);
            transactionInputs = transactionsRequest.stream()
                    .map(txnReq -> {
                        var requestKind = txnReq.getKind();
                        var parentTransaction = determineParentTransactionInRequest(txnReq, transactionsRequest);
                        var kind = OrderTransaction.Kind.capture.equals(requestKind)
                                && (Objects.isNull(parentTransaction) || OrderTransaction.Kind.sale.equals(parentTransaction.getKind()))
                                ? OrderTransaction.Kind.sale : requestKind;
                        var status = OrderTransaction.Kind.capture.equals(requestKind)
                                && (Objects.isNull(parentTransaction) || OrderTransaction.Kind.sale.equals(parentTransaction.getKind()))
                                ? OrderTransaction.Status.success : txnReq.getStatus();

                        return Order.TransactionInput.builder()
                                .kind(kind)
                                .status(status)
                                .amount(txnReq.getAmount())
                                .authorization(txnReq.getAuthorization())
                                .gateway(txnReq.getGateway())
                                .build();
                    })
                    .toList();
        }

        return OrderPaymentResult.builder()
                .isFromCheckout(isFromCheckout)
                .checkoutToken(checkoutToken)
                .paymentIds(paymentIds)
                .transactions(transactionInputs)
                .build();
    }

    private TransactionCreateRequest determineParentTransactionInRequest(
            TransactionCreateRequest transaction,
            List<TransactionCreateRequest> transactionsRequest
    ) {
        // Tìm authorization, sale, capture success đầu tiên
        int indexFirstAuthorization = -1;
        int indexFirstSalePending = -1;
        int indexFirstSale = -1;
        int indexFirstCaptureSuccess = -1;

        for (int i = 0; i < transactionsRequest.size(); i++) {
            var transactionCreateRequest = transactionsRequest.get(i);

            if (indexFirstAuthorization == -1
                    && transactionCreateRequest.getKind() == OrderTransaction.Kind.authorization) {
                indexFirstAuthorization = i;
            }
            if (indexFirstSalePending == -1
                    && transactionCreateRequest.getKind() == OrderTransaction.Kind.sale
                    && transactionCreateRequest.getStatus() == OrderTransaction.Status.pending) {
                indexFirstSalePending = i;
            }
            if (indexFirstSale == -1
                    && transactionCreateRequest.getKind() == OrderTransaction.Kind.sale) {
                indexFirstSale = i;
            }
            if (indexFirstCaptureSuccess == -1
                    && transactionCreateRequest.getKind() == OrderTransaction.Kind.capture
                    && transactionCreateRequest.getStatus() == OrderTransaction.Status.success) {
                indexFirstCaptureSuccess = i;
            }

            if (indexFirstAuthorization != -1 && indexFirstSale != -1) {
                break;
            }
        }

        return switch (transaction.getKind()) {
            case capture ->
                    findCaptureParentTransaction(transactionsRequest, indexFirstAuthorization, indexFirstSalePending);
            case _void ->
                    findVoidParentTransaction(transactionsRequest, indexFirstAuthorization, indexFirstSalePending);
            case refund -> findRefundParentTransaction(transactionsRequest, indexFirstSale, indexFirstCaptureSuccess);
            default -> null;
        };
    }

    private TransactionCreateRequest findRefundParentTransaction(
            List<TransactionCreateRequest> transactionsRequest,
            int indexFirstSale,
            int indexFirstCaptureSuccess
    ) {
        if (indexFirstSale == -1 && indexFirstCaptureSuccess == -1) {
            return null;
        } else if (indexFirstSale == -1) {
            return transactionsRequest.get(indexFirstCaptureSuccess);
        } else {
            return transactionsRequest.get(indexFirstSale);
        }
    }

    private TransactionCreateRequest findVoidParentTransaction(
            List<TransactionCreateRequest> transactionsRequest,
            int indexFirstAuthorization,
            int indexFirstSalePending
    ) {
        if (indexFirstAuthorization == -1 && indexFirstSalePending == -1) {
            return null;
        } else if (indexFirstAuthorization != -1) {
            return transactionsRequest.get(indexFirstAuthorization);
        } else {
            return transactionsRequest.get(indexFirstSalePending);
        }
    }

    private TransactionCreateRequest findCaptureParentTransaction(
            List<TransactionCreateRequest> transactionsRequest,
            int indexFirstAuthorization,
            int indexFirstSalePending
    ) {
        if (indexFirstAuthorization == -1 && indexFirstSalePending == -1) {
            return null;
        } else if (indexFirstAuthorization != -1) {
            return transactionsRequest.get(indexFirstAuthorization);
        } else {
            return transactionsRequest.get(indexFirstSalePending);
        }
    }

    private void preCheckTransactionsInput(List<TransactionCreateRequest> transactionsRequest) {
        for (int i = 0; i < transactionsRequest.size(); i++) {
            var request = transactionsRequest.get(i);
            if (request.getAmount() == null) {
                var resolvedErrorMessage = messageSource.getMessage(
                        "transaction.error.create.amount.required",
                        null,
                        LocaleContextHolder.getLocale()
                );
                throw new ConstrainViolationException(UserError.builder()
                        .code("required")
                        .fields(List.of("transactions", String.valueOf(i), "amount"))
                        .message(resolvedErrorMessage)
                        .build());
            }
        }
    }

    private OrderRoutingResponse processOrderRouting(Order order, Location location) {
        if (order.getLocationId() != null) {
            return routingToSpecificLocation(order, location);
        } else {
            return processOrderRoutingRequest(order);
        }
    }

    private OrderRoutingResponse processOrderRoutingRequest(Order order) {
        var storeId = order.getId().getStoreId();
        var orderRoutingItemRequest = order.getLineItems().stream()
                .map(item -> OrderRoutingItemRequest.builder()
                        .variantId(item.getVariantInfo().getVariantId())
                        .quantity(BigDecimal.valueOf(item.getQuantity()))
                        .requiresShipping(item.getVariantInfo().isRequireShipping())
                        .build())
                .toList();
        return new OrderRoutingResponse(List.of());
    }

    private OrderRoutingResponse routingToSpecificLocation(Order order, Location location) {
        OrderRoutingResponse.OrderRoutingLocation orderRoutingLocation = OrderRoutingResponse.OrderRoutingLocation.builder()
                .id(location.getId())
                .address(location.getAddress())
                .name(location.getName())
                .phone(location.getPhone())
                .email(location.getEmail())
                .build();

        List<OrderRoutingResponse.IndexesItem> indexesItems = new ArrayList<>();
        for (int i = 0; i < order.getLineItems().size(); i++) {
            var orderLineItem = order.getLineItems().get(i);
            Integer variantId = orderLineItem.getVariantInfo().getVariantId();
            Integer inventoryItemId = ConvertUtils.toInt(orderLineItem.getVariantInfo().getInventoryItemId());
            var indexItem = OrderRoutingResponse.IndexesItem.builder()
                    .variantId(variantId)
                    .inventoryItemId(inventoryItemId)
                    .index(i)
                    .build();
            indexesItems.add(indexItem);
        }

        var orderRoutingResult = OrderRoutingResponse.OrderRoutingResult.builder()
                .location(orderRoutingLocation)
                .indexesItems(indexesItems)
                .build();

        return new OrderRoutingResponse(List.of(orderRoutingResult));
    }


    private Location validateLocation(Integer storeId, OrderCreateRequest orderRequest) {
        var locationId = orderRequest.getLocationId();
        if (locationId != null) {
            var location = locationDao.getById(storeId, locationId);
            if (location == null)
                throw new NotFoundException("location not found");
            return location;
        }
        return null;
    }

    private void handleTaxLinesOrderRequest(
            OrderCreateRequest request,
            List<LineItem> lineItems,
            List<DiscountAllocation> discountAllocations,
            Currency currency
    ) {
        if (CollectionUtils.isEmpty(request.getTaxLines())) return;

        var discountLineItems = discountAllocations == null
                ? new ArrayList<DiscountAllocation>()
                : discountAllocations.stream().filter(d -> d.getTargetType() == DiscountAllocation.TargetType.line_item).toList();

        var taxLineRemainingMap = new HashMap<String, OrderCreateRequest.TaxLineRequest>();
        var taxableLineItems = new ArrayList<LineItem>();
        var freeLineItems = new ArrayList<LineItem>();
        var remainingLineItemPrice = new HashMap<Integer, BigDecimal>();

        for (var taxLineRequest : request.getTaxLines()) {
            var taxLine = taxLineRemainingMap.get(taxLineRequest.getKey());
            if (taxLine == null) {
                taxLineRemainingMap.put(taxLineRequest.getTitle(), taxLineRequest);
            } else {
                taxLine.addPrice(taxLine.getPrice());
            }
        }

        for (var lineItem : lineItems) {
            var totalDiscountOfLine = discountLineItems.stream()
                    .filter(d -> d.getTargetId() == lineItem.getId())
                    .map(DiscountAllocation::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var taxLinesOfLine = lineItem.getTaxLines() == null ? BigDecimal.ZERO
                    : lineItem.getTaxLines().stream().map(TaxLine::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

            var reducePrice = totalDiscountOfLine.add(taxLinesOfLine);
            remainingLineItemPrice.put(lineItem.getId(), lineItem.getOriginalTotal().subtract(reducePrice).max(BigDecimal.ZERO));
            if (lineItem.getOriginalTotal().compareTo(reducePrice) > 0) {
                taxableLineItems.add(lineItem);
            } else {
                freeLineItems.add(lineItem);
            }
        }

        var totalRemainingPrice = remainingLineItemPrice.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        var taxLineIds = orderIdGenerator.generateTaxLineIds(request.getTaxLines().size() * lineItems.size());
        int lastLineIndex = taxableLineItems.size() - 1;
        for (int i = 0; i < taxableLineItems.size(); i++) {
            var lineItem = taxableLineItems.get(i);
            var taxLines = buildTaxLinesForLineItem(
                    lineItem,
                    taxLineIds,
                    remainingLineItemPrice.get(lineItem.getId()),
                    totalRemainingPrice,
                    taxLineRemainingMap,
                    currency,
                    i == lastLineIndex
            );
            lineItem.applyTax(taxLines);
        }

        for (var lineItem : freeLineItems) {
            var taxLines = taxLineRemainingMap.values().stream()
                    .map(taxLineRequest -> new TaxLine(
                            taxLineIds.removeFirst(),
                            taxLineRequest.getRate(),
                            taxLineRequest.getTitle(),
                            BigDecimal.ZERO,
                            lineItem.getId(),
                            TaxLine.TargetType.line_item,
                            lineItem.getQuantity()
                    )).toList();
            lineItem.applyTax(taxLines);
        }
    }

    private List<TaxLine> buildTaxLinesForLineItem(
            LineItem lineItem,
            Deque<Integer> taxLineIds,
            BigDecimal totalRemainingPrice,
            BigDecimal remainingPrice,
            HashMap<String, OrderCreateRequest.TaxLineRequest> taxLineRemainingMap,
            Currency currency,
            boolean useRemainingPrice
    ) {
        var taxLines = new ArrayList<TaxLine>();
        if (useRemainingPrice) {
            taxLineRemainingMap.values().forEach(taxLineRequest -> {
                var taxLine = new TaxLine(
                        taxLineIds.removeFirst(),
                        taxLineRequest.getRate(),
                        taxLineRequest.getTitle(),
                        taxLineRequest.getPrice(),
                        lineItem.getId(),
                        TaxLine.TargetType.line_item,
                        lineItem.getQuantity()
                );
                taxLines.add(taxLine);
            });
        } else {
            for (var taxLineRequest : taxLineRemainingMap.values()) {
                var price = taxLineRequest.getPrice()
                        .multiply(remainingPrice)
                        .divide(totalRemainingPrice, currency.getDefaultFractionDigits(), RoundingMode.UP);
                var taxLine = new TaxLine(
                        taxLineIds.removeFirst(),
                        taxLineRequest.getRate(),
                        taxLineRequest.getTitle(),
                        price,
                        lineItem.getId(),
                        TaxLine.TargetType.line_item,
                        lineItem.getQuantity()
                );
                taxLines.add(taxLine);

                var remainingTaxLinePrice = taxLineRequest.getPrice().subtract(price).max(BigDecimal.ZERO);
                taxLineRequest.setPrice(remainingTaxLinePrice);
            }
        }

        return taxLines;
    }

    private Pair<List<DiscountAllocation>, List<DiscountApplication>> allocateDiscounts(
            boolean isFromTrustedSource,
            List<OrderDiscountCode> discountCodes,
            Currency currency,
            List<LineItem> lineItems,
            List<ShippingLine> shippingLines,
            OrderCreateRequest orderRequest
    ) {
        var applicationRequests = CollectionUtils.isEmpty(orderRequest.getDiscountApplications())
                ? new ArrayList<OrderCreateRequest.DiscountApplicationRequest>()
                : orderRequest.getDiscountApplications();
        var allocationRequests = new ArrayList<OrderCreateRequest.DiscountAllocationRequest>();

        int lineIndex = 0;
        for (var lineItem : lineItems) {
            var lineItemRequest = orderRequest.getLineItems().get(lineIndex++);
            var lineDiscountAllocationRequests = lineItemRequest.getDiscountAllocations();
            if (isFromTrustedSource) {
                if (CollectionUtils.isEmpty(lineDiscountAllocationRequests)) continue;
                for (var allocationRequest : lineDiscountAllocationRequests) {
                    allocationRequest.setTargetId(lineItem.getId());
                    allocationRequest.setTargetType(DiscountAllocation.TargetType.line_item);
                    allocationRequests.add(allocationRequest);
                }
            } else {
                if (!NumberUtils.isPositive(lineItemRequest.getTotalDiscount())) continue;
                var applicationIndex = applicationRequests.size();
                var lineDiscountApplicationRequest = OrderCreateRequest.DiscountApplicationRequest.builder()
                        .value(lineItemRequest.getTotalDiscount().min(lineItem.getSubtotalLinePrice()))
                        .valueType(DiscountApplication.ValueType.fixed_amount)
                        .targetType(DiscountApplication.TargetType.line_item)
                        .index(applicationIndex)
                        .code("manual")
                        .title("manual")
                        .description("manual")
                        .build();
                applicationRequests.add(lineDiscountApplicationRequest);
                var lineDiscountAllocationRequest = OrderCreateRequest.DiscountAllocationRequest.builder()
                        .amount(lineItemRequest.getTotalDiscount().min(lineItem.getSubtotalLinePrice()))
                        .targetId(lineItem.getId())
                        .targetType(DiscountAllocation.TargetType.line_item)
                        .discountApplicationIndex(applicationIndex)
                        .build();
                allocationRequests.add(lineDiscountAllocationRequest);
            }
        }

        if (!CollectionUtils.isEmpty(shippingLines)) {
            lineIndex = 0;
            for (var shippingLine : shippingLines) {
                var shippingLineRequest = orderRequest.getShippingLines().get(lineIndex++);
                if (CollectionUtils.isEmpty(shippingLineRequest.getDiscountAllocations())) continue;
                for (var allocationRequest : shippingLineRequest.getDiscountAllocations()) {
                    allocationRequest.setTargetType(DiscountAllocation.TargetType.shipping_line);
                    allocationRequest.setTargetId(shippingLine.getId());
                    allocationRequests.add(allocationRequest);
                }
            }
        }

        if (!CollectionUtils.isEmpty(discountCodes)) {
            var discountCode = discountCodes.get(0);
            if (!isFromTrustedSource) {
                var applicationIndex = allocationRequests.size();
                var applicationRequestBuilder = OrderCreateRequest.DiscountApplicationRequest.builder()
                        .index(applicationIndex)
                        .title(discountCode.getCode())
                        .code(discountCode.getCode())
                        .ruleType(DiscountApplication.RuleType.order);
                var discountCodeRequest = orderRequest.getDiscountCodes().get(0);
                applicationRequestBuilder
                        .code(null)
                        .title(StringUtils.firstNonBlank(discountCodeRequest.getTitle(), discountCode.getCode()))
                        .description(discountCodeRequest.getDescription());
                switch (discountCode.getType()) {
                    case shipping_line -> {
                        if (!CollectionUtils.isEmpty(shippingLines)) {
                            var totalShippingLinePrice = shippingLines.stream().map(ShippingLine::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                            var applicationRequest = applicationRequestBuilder
                                    .targetType(DiscountApplication.TargetType.shipping_line);
                            if (discountCode.getAmount().compareTo(totalShippingLinePrice) >= 0) {
                                applicationRequest
                                        .valueType(DiscountApplication.ValueType.percentage)
                                        .value(BigDecimals.ONE_HUNDRED);
                            } else {
                                applicationRequest
                                        .valueType(DiscountApplication.ValueType.fixed_amount)
                                        .value(discountCode.getAmount());
                            }
                            applicationRequests.add(applicationRequest.build());

                            BigDecimal calculateDiscountAmount = BigDecimal.ZERO;
                            int lastIndex = shippingLines.size() - 1;
                            int i = 0;
                            for (var shippingLine : shippingLines) {
                                BigDecimal amount;
                                if (i != lastIndex) {
                                    amount = discountCode.getAmount()
                                            .multiply(shippingLine.getPrice())
                                            .divide(totalShippingLinePrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
                                    amount = amount.min(shippingLine.getPrice());
                                    calculateDiscountAmount = calculateDiscountAmount.add(amount);
                                } else {
                                    amount = totalShippingLinePrice.subtract(calculateDiscountAmount);
                                }
                                var allocationRequest = OrderCreateRequest.DiscountAllocationRequest.builder()
                                        .discountApplicationIndex(applicationIndex)
                                        .targetType(DiscountAllocation.TargetType.shipping_line)
                                        .targetId(shippingLine.getId())
                                        .amount(amount.min(shippingLine.getPrice()))
                                        .build();
                                allocationRequests.add(allocationRequest);
                                i++;
                            }
                        }
                    }
                    case percentage, fixed_amount -> {
                        var applicationRequest = applicationRequestBuilder
                                .valueType(DiscountApplication.ValueType.valueOf(discountCode.getType().name()))
                                .value(discountCode.getValue())
                                .targetType(DiscountApplication.TargetType.line_item)
                                .build();
                        applicationRequests.add(applicationRequest);

                        var subtotalLineItemPrice = lineItems.stream().map(LineItem::getSubtotalLinePrice).reduce(BigDecimal.ZERO, BigDecimal::add);

                        if (lineItems.size() == 1) {
                            var lineItem = lineItems.get(0);
                            var amount = switch (applicationRequest.getValueType()) {
                                case percentage -> lineItem.getSubtotalLinePrice()
                                        .multiply(discountCode.getAmount())
                                        .divide(BigDecimals.ONE_HUNDRED, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
                                case fixed_amount -> lineItem.getSubtotalLinePrice().min(discountCode.getAmount());
                            };
                            var allocationRequest = OrderCreateRequest.DiscountAllocationRequest.builder()
                                    .discountApplicationIndex(applicationIndex)
                                    .targetType(DiscountAllocation.TargetType.line_item)
                                    .targetId(lineItem.getId())
                                    .amount(amount)
                                    .build();
                            allocationRequests.add(allocationRequest);
                        } else {
                            var eligibleLineItems = lineItems.stream().filter(l -> NumberUtils.isPositive(l.getSubtotalLinePrice())).toList();
                            if (!CollectionUtils.isEmpty(eligibleLineItems)) {
                                var lineItemCount = eligibleLineItems.size();
                                var lastIndex = lineItemCount - 1;

                                switch (applicationRequest.getValueType()) {
                                    case percentage -> {
                                        for (LineItem lineItem : eligibleLineItems) {
                                            BigDecimal amount = lineItem.getSubtotalLinePrice()
                                                    .multiply(discountCode.getAmount())
                                                    .divide(BigDecimals.ONE_HUNDRED,
                                                            currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
                                            var allocationRequest = OrderCreateRequest.DiscountAllocationRequest.builder()
                                                    .discountApplicationIndex(applicationIndex)
                                                    .targetType(DiscountAllocation.TargetType.line_item)
                                                    .targetId(lineItem.getId())
                                                    .amount(amount)
                                                    .build();
                                            allocationRequests.add(allocationRequest);
                                        }
                                    }
                                    case fixed_amount -> {
                                        var calculatedDiscountAmount = BigDecimal.ZERO;
                                        for (int i = 0; i < lineItemCount; i++) {
                                            var lineItem = eligibleLineItems.get(i);

                                            BigDecimal amount;
                                            if (i != lastIndex) {
                                                amount = lineItem.getSubtotalLinePrice()
                                                        .multiply(discountCode.getAmount())
                                                        .divide(subtotalLineItemPrice,
                                                                currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
                                                calculatedDiscountAmount = calculatedDiscountAmount.add(amount);
                                            } else {
                                                amount = discountCode.getAmount()
                                                        .subtract(calculatedDiscountAmount)
                                                        .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
                                            }
                                            var allocationRequest = OrderCreateRequest.DiscountAllocationRequest.builder()
                                                    .discountApplicationIndex(applicationIndex)
                                                    .targetType(DiscountAllocation.TargetType.line_item)
                                                    .targetId(lineItem.getId())
                                                    .amount(amount)
                                                    .build();
                                            allocationRequests.add(allocationRequest);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (CollectionUtils.isEmpty(applicationRequests) && CollectionUtils.isEmpty(allocationRequests))
            return Pair.of(null, null);

        var index = new AtomicInteger();
        var discountApplicationIds = orderIdGenerator.generateDiscountApplicationIds(applicationRequests.size());
        var applications = applicationRequests.stream()
                .map(request ->
                        new DiscountApplication(
                                discountApplicationIds.removeFirst(),
                                index.getAndIncrement(),
                                request.getValue(),
                                request.getValueType(),
                                request.getTargetType(),
                                request.getRuleType())
                                .setDiscountNames(request.getCode(), request.getTitle(), request.getDescription())
                ).toList();
        var discountAllocationIds = orderIdGenerator.generateDiscountAllocationIds(allocationRequests.size());
        var allocations = allocationRequests.stream()
                .map(request ->
                        new DiscountAllocation(
                                discountAllocationIds.removeFirst(),
                                request.getAmount(),
                                request.getTargetId(),
                                request.getTargetType(),
                                applications.stream()
                                        .filter(a -> a.getApplyIndex() == request.getDiscountApplicationIndex())
                                        .map(DiscountApplication::getId).findFirst().orElseThrow(),
                                request.getDiscountApplicationIndex()
                        )
                ).toList();

        return Pair.of(allocations, applications);
    }


    private List<OrderDiscountCode> buildOrderDiscountCodes(
            List<OrderCreateRequest.DiscountCodeRequest> requests,
            BigDecimal totalDiscount,
            List<LineItem> lineItems,
            List<ShippingLine> shippingLines,
            Currency currency
    ) {
        var totalLineItemPrice = lineItems.stream().map(LineItem::getSubtotalLinePrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalShippingPrice = shippingLines.stream().map(ShippingLine::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (CollectionUtils.isEmpty(requests)) {
            if (!NumberUtils.isPositive(totalDiscount)) return List.of();
            return List.of(
                    new OrderDiscountCode(
                            orderIdGenerator.generateOrderDiscountCodeId(),
                            "custom-discount",
                            OrderDiscountCode.ValueType.fixed_amount,
                            totalDiscount.min(totalLineItemPrice).setScale(currency.getDefaultFractionDigits(), RoundingMode.UP)
                            , true,
                            true
                    )
            );
        }

        // chỉ apply trên 1 discount code
        var request = requests.get(0);
        if (request.getAmount() == null || StringUtils.isBlank(request.getCode())) return List.of();
        if (!NumberUtils.isPositive(request.getAmount())) {
            throw new ConstrainViolationException(
                    "discount_codes",
                    "must be greater than or equal to 0"
            );
        }

        var discountCodeId = orderIdGenerator.generateOrderDiscountCodeId();
        boolean isCustom = true;
        var discount = switch (request.getType()) {
            case fixed_amount -> new OrderDiscountCode(
                    discountCodeId,
                    request.getCode(),
                    OrderDiscountCode.ValueType.fixed_amount,
                    request.getAmount().min(totalLineItemPrice).setScale(currency.getDefaultFractionDigits(), RoundingMode.DOWN),
                    isCustom,
                    true);
            case shipping_line -> new OrderDiscountCode(
                    discountCodeId,
                    request.getCode(),
                    OrderDiscountCode.ValueType.shipping_line,
                    request.getAmount().min(totalShippingPrice).setScale(currency.getDefaultFractionDigits(), RoundingMode.DOWN),
                    isCustom,
                    true);
            case percentage -> {
                boolean isPrecalculate = false;
                yield new OrderDiscountCode(
                        discountCodeId,
                        request.getCode(),
                        OrderDiscountCode.ValueType.percentage,
                        request.getAmount(),
                        isCustom,
                        isPrecalculate);
            }
        };

        return List.of(discount);
    }

    private List<ShippingLine> buildShippingLines(Integer storeId, List<OrderCreateRequest.ShippingLineRequest> shippingLines) {
        if (CollectionUtils.isEmpty(shippingLines)) return List.of();
        return shippingLines.stream().map(this::buildShippingLine).toList();
    }

    private ShippingLine buildShippingLine(OrderCreateRequest.ShippingLineRequest request) {
        var shippingLineId = orderIdGenerator.generateShippingLineId();
        var shippingLine = new ShippingLine(
                shippingLineId,
                request.getCode(),
                request.getTitle(),
                request.getSource(),
                request.getPrice()
        );

        if (!CollectionUtils.isEmpty(shippingLine.getTaxLines())) {
            var taxLineIds = orderIdGenerator.generateTaxLineIds(shippingLine.getTaxLines().size());
            var taxLines = shippingLine.getTaxLines().stream()
                    .map(line ->
                            new TaxLine(
                                    taxLineIds.removeFirst(),
                                    line.getRate(),
                                    line.getTitle(),
                                    line.getPrice(),
                                    shippingLineId,
                                    TaxLine.TargetType.shipping_line,
                                    0)
                    )
                    .toList();
            shippingLine.applyTax(taxLines);
        }

        return shippingLine;
    }

    private List<LineItem> buildLineItems(
            Integer storeId,
            List<OrderCreateRequest.LineItemRequest> lineItemRequests,
            List<CombinationLine> combinationLines,
            Currency currency
    ) {
        if (CollectionUtils.isEmpty(lineItemRequests))
            throw new IllegalArgumentException("line_items must be not empty or null");

        var variantIds = lineItemRequests.stream()
                .map(OrderCreateRequest.LineItemRequest::getVariantId)
                .filter(NumberUtils::isPositive)
                .distinct().toList();
        var variants = productDao.findVariantByListIds(storeId, variantIds);
        var productIds = variants.stream().map(VariantDto::getProductId).distinct().toList();
        var products = productDao.findProductByListIds(storeId, productIds);

        var generatedIds = orderIdGenerator.generateLineItemIds(lineItemRequests.size());
        List<LineItem> linetems = new ArrayList<>(lineItemRequests.size());
        for (var request : lineItemRequests) {
            VariantDto variant = null;
            ProductDto product = null;
            Integer combinationLineId = null;
            if (NumberUtils.isPositive(request.getVariantId())) {
                variant = variants.stream().filter(v -> v.getId() == request.getVariantId()).findFirst().orElse(null);
                if (variant != null) {
                    var productId = variant.getProductId();
                    product = products.stream().filter(p -> p.getId() == productId).findFirst().orElse(null);
                }
            }
            if (variant == null && !NumberUtils.isPositive(request.getPrice()) && StringUtils.isBlank(request.getTitle())) {
                throw new ConstrainViolationException("line_item",
                        String.format("line_item[%s] is custom must be fill price and title", lineItemRequests.indexOf(request)));
            }

            var combinationIndex = request.getCombinationLineIndex();
            var combinationSize = combinationLines.size();
            if (combinationIndex != null && combinationIndex >= 0 && combinationIndex < combinationSize) {
                combinationLineId = combinationLines.get(combinationIndex).getId();
            }

            var lineItem = buildLineItem(generatedIds.removeFirst(), request, variant, product, combinationLineId, currency);
            linetems.add(lineItem);
        }

        return linetems;
    }

    private LineItem buildLineItem(
            int id,
            OrderCreateRequest.LineItemRequest request,
            VariantDto variant,
            ProductDto product,
            Integer combinationLineId,
            Currency currency
    ) {
        boolean productExisted = false;

        Integer variantId = null;
        Integer productId = null;
        String title = request.getTitle();
        String variantTitle = request.getVariantTitle();
        String vendor = request.getVendor();
        String sku = request.getSku();
        String discountCode = null;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal price = NumberUtils.isPositive(request.getPrice())
                ? request.getPrice().setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP)
                : request.getPrice();
        int weight = request.getGrams();
        String inventoryManagement = null;
        Boolean requireShipping = request.getRequireShipping();
        List<TaxLine> taxLines = buildTaxLines(id, request.getQuantity(), request.getTaxLines());
        Boolean taxable = request.getTaxable();
        Long inventoryItemId = null;
        String unit = request.getUnit();

        if (variant != null && product != null) {
            productExisted = true;
            variantId = variant.getId();
            productId = product.getId();
            inventoryManagement = variant.getInventoryManagement();

            if (requireShipping != null) requireShipping = variant.isRequiresShipping();
            inventoryItemId = variant.getInventoryItemId();

            if (StringUtils.isBlank(title)) title = product.getName();
            if (StringUtils.isBlank(variantTitle)) variantTitle = variant.getTitle();
            if (StringUtils.isBlank(sku)) sku = variant.getSku();
            if (StringUtils.isBlank(vendor)) vendor = product.getVendor();

            if (price == null) {
                price = variant.getPrice().setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
            }
            if (weight == 0) weight = variant.getWeight();
            if (taxable == null) taxable = variant.isTaxable();
        }

        if (requireShipping == null) requireShipping = true;
        if (price != null && request.getTotalDiscount() != null) {
            discountCode = request.getDiscountCode();
            discount = request.getTotalDiscount().min(price);
        }
        taxable = taxable != null && taxable;

        var componentVariantId = variantId != null ? variantId : request.getVariantId();
        var combinationLineKey = NumberUtils.isPositive(combinationLineId) && NumberUtils.isPositive(componentVariantId)
                ? String.format("%s-%s", combinationLineId, componentVariantId)
                : null;

        var variantInfo = VariantInfo.builder()
                .productId(productId)
                .variantId(variantId)
                .productExisted(productExisted)
                .title(title)
                .variantTitle(variantTitle)
                .vendor(vendor)
                .sku(sku)
                .grams(weight)
                .requireShipping(requireShipping)
                .variantInventoryManagement(inventoryManagement)
                .restockable(inventoryManagement != null)
                .inventoryItemId(inventoryItemId)
                .unit(unit)
                .build();

        if (price == null) {
            throw new ConstrainViolationException("line_item", "price must be not null");
        }

        return new LineItem(
                id,
                request.getQuantity(),
                price,
                discount,
                discountCode,
                variantInfo,
                taxable,
                taxLines,
                "manual",
                false,
                combinationLineKey);
    }

    private List<TaxLine> buildTaxLines(int targetId, int quantity, List<OrderCreateRequest.TaxLineRequest> taxLines) {
        if (CollectionUtils.isEmpty(taxLines)) return List.of();

        var ids = orderIdGenerator.generateTaxLineIds(taxLines.size());
        return taxLines.stream()
                .map(line ->
                        new TaxLine(
                                ids.removeFirst(),
                                line.getRate(),
                                line.getTitle(),
                                line.getPrice(),
                                targetId,
                                TaxLine.TargetType.line_item,
                                quantity)
                )
                .toList();
    }

    private List<CombinationLine> buildCombinationLines(OrderCreateRequest orderRequest) {
        if (CollectionUtils.isEmpty(orderRequest.getCombinationLines())) return List.of();
        var ids = orderIdGenerator.generateCombinationLineIds(orderRequest.getCombinationLines().size());
        return orderRequest.getCombinationLines().stream()
                .map(line ->
                        new CombinationLine(
                                ids.removeFirst(),
                                line.getVariantId(),
                                line.getProductId(),
                                line.getPrice(),
                                line.getQuantity(),
                                line.getTitle(),
                                line.getVariantTitle(),
                                line.getSku(),
                                line.getVendor(),
                                line.getUnit(),
                                line.getItemUnit(),
                                line.getType())
                ).toList();
    }

    private Pair<BillingAddress, ShippingAddress> buildAddress(
            OrderCreateRequest.AddressRequest billingAddressRequest,
            OrderCreateRequest.AddressRequest shippingAddressRequest
    ) {
        BillingAddress billingAddress = null;
        ShippingAddress shippingAddress = null;
        if (billingAddressRequest != null) {
            var billingAddressId = orderIdGenerator.generateBillingAddressId();
            billingAddress = new BillingAddress(billingAddressId, resolveAddress(billingAddressRequest));
        }
        if (shippingAddressRequest != null) {
            var shippingAddressId = orderIdGenerator.generateShippingAddressId();
            shippingAddress = new ShippingAddress(shippingAddressId, resolveAddress(shippingAddressRequest));
        }
        return Pair.of(billingAddress, shippingAddress);
    }

    private OrderCustomerContext resolveCustomer(Integer storeId, OrderCreateRequest request) {
        var ctx = new OrderCustomerContext();

        if ((request.getCustomer() == null || request.getCustomer().isEmpty())
                && StringUtils.isBlank(request.getEmail())
                && StringUtils.isBlank(request.getPhone())) {
            return ctx;
        }

        ctx.setEmail(request.getEmail());
        ctx.setPhone(request.getPhone());

        if (request.getCustomer() != null && !request.getCustomer().isEmpty()) {
            if (StringUtils.isNotBlank(request.getCustomer().getEmail())) {
                ctx.setEmail(request.getCustomer().getEmail());
            }
            if (StringUtils.isNotBlank(request.getCustomer().getPhone())) {
                ctx.setPhone(request.getCustomer().getPhone());
            }
        }

        var apiCustomer = customerService.findById(storeId, request.getCustomer() == null ? 0 : request.getCustomer().getId());
        var emailCheckedCustomer = customerService.findByEmail(storeId, ctx.getEmail());
        if (emailCheckedCustomer != null) {
            if (apiCustomer == null) {
                apiCustomer = emailCheckedCustomer;
            } else if (apiCustomer.getId() != apiCustomer.getId()) {
                ctx.setEmail(apiCustomer.getEmail());

                if (StringUtils.isNotBlank(request.getEmail())
                        && StringUtils.isNotBlank(apiCustomer.getEmail())
                        && !StringUtils.equals(request.getEmail(), apiCustomer.getEmail())) {
                    request.setEmail(apiCustomer.getEmail());
                }
            }
        }

        String updatablePhone;
        if (apiCustomer == null || StringUtils.isBlank(apiCustomer.getPhone())) {
            if (StringUtils.isNotBlank(ctx.getPhone()) && CustomerPhoneUtils.isValid(ctx.getPhone())) {
                updatablePhone = CustomerPhoneUtils.normalize(ctx.getPhone());
                var phoneCheckedCustomer = customerService.findByPhone(storeId, updatablePhone);
                if (phoneCheckedCustomer == null || (apiCustomer != null && apiCustomer.getId() == phoneCheckedCustomer.getId())) {
                    ctx.setPhone(updatablePhone);
                }
                if (apiCustomer == null && phoneCheckedCustomer != null) {
                    apiCustomer = phoneCheckedCustomer;
                }
            }
        }

        var address = request.getBillingAddress() == null ? null : resolveAddress(request.getBillingAddress());
        var fullName = address != null
                ? Pair.of(address.getFirstName(), address.getLastName())
                : Pair.<String, String>of(null, null);

        if (StringUtils.isNotBlank(ctx.getEmail()) || StringUtils.isNotBlank(ctx.getPhone()) || StringUtils.isNotBlank(fullName.getLeft())) {
            if (apiCustomer == null) {
                apiCustomer = customerService.create(storeId, ctx.getEmail(), ctx.getPhone(), fullName, address);
                ctx.setFirstTimeCustomer(true);
            } else {
                var updatedEmail = Optional.ofNullable(apiCustomer.getEmail()).orElse(ctx.getEmail());
                var updatedPhone = Optional.ofNullable(apiCustomer.getPhone()).orElse(ctx.getPhone());
                boolean shouldUpdateCustomer = !StringUtils.equals(ctx.getEmail(), apiCustomer.getEmail())
                        || !StringUtils.equals(ctx.getPhone(), apiCustomer.getPhone());
                if (shouldUpdateCustomer) {
                    apiCustomer = customerService.update(storeId, apiCustomer.getId(), updatedEmail, updatedPhone);
                }

            }
        }

        if (apiCustomer != null) {
            ctx.setCustomerId(apiCustomer.getId());
            ctx.setAcceptsMarketing(apiCustomer.isAcceptsMarketing());
            if (StringUtils.isBlank(ctx.getEmail())) {
                ctx.setEmail(apiCustomer.getEmail());
            }
            if (StringUtils.isBlank(ctx.getPhone())) {
                ctx.setPhone(apiCustomer.getPhone());
            }
        }

        return ctx;
    }

    private MailingAddress resolveAddress(OrderCreateRequest.AddressRequest address) {
        var names = extractFullName(address);
        var area = AddressHelper.resolve(orderMapper.toAddressRequest(address));
        return new MailingAddress(
                names.getLeft(),
                names.getRight(),
                address.getPhone(),
                address.getAddress(),
                address.getCompany(),
                address.getCity(),
                area.getCountry(),
                area.getProvince(),
                area.getDistrict(),
                area.getWard());
    }

    private Pair<String, String> extractFullName(OrderCreateRequest.AddressRequest address) {
        if (StringUtils.isNotBlank(address.getName())) {
            return AddressHelper.getStructureName(address.getName());
        }
        return Pair.of(address.getFirstName(), address.getLastName());
    }

    private void validateTaxLineRequest(OrderCreateRequest orderRequest, Currency currency) {
        var isOrderHasTaxLines = !CollectionUtils.isEmpty(orderRequest.getTaxLines());
        var lineItemHasTaxLines = orderRequest.getLineItems().stream()
                .filter(line -> !CollectionUtils.isEmpty(line.getTaxLines()))
                .toList();

        // taxLines chỉ có thể trong order hoặc lineItems, không trong cả 2
        if (isOrderHasTaxLines && !CollectionUtils.isEmpty(lineItemHasTaxLines))
            throw new ConstrainViolationException("tax_line", "must fill taxLines in Order or in Order.LineItems only not both of them");

        if (!CollectionUtils.isEmpty(orderRequest.getShippingLines())) {
            for (var shippingLine : orderRequest.getShippingLines()) {
                if (CollectionUtils.isEmpty(shippingLine.getTaxLines())) continue;
                var mergedTaxLines = mergeTaxLines(shippingLine.getTaxLines(), currency);
                shippingLine.setTaxLines(mergedTaxLines);
            }
        }

        if (isOrderHasTaxLines) {
            var validTaxLines = mergeTaxLines(orderRequest.getTaxLines(), currency);
            orderRequest.setTaxLines(validTaxLines);
        }

        if (!CollectionUtils.isEmpty(lineItemHasTaxLines)) {
            for (var lineItem : lineItemHasTaxLines) {
                var mergedTaxLines = mergeTaxLines(lineItem.getTaxLines(), currency);
                lineItem.setTaxLines(mergedTaxLines);
            }
        }
    }

    // merge taxline có dùng rate và title
    private List<OrderCreateRequest.TaxLineRequest> mergeTaxLines(List<OrderCreateRequest.TaxLineRequest> taxLines, Currency currency) {
        var validTaxLines = new HashMap<String, OrderCreateRequest.TaxLineRequest>();
        for (var taxLine : taxLines) {
            var validTaxLine = validTaxLines.get(taxLine.getKey());
            if (validTaxLine == null) {
                validTaxLines.put(taxLine.getKey(), taxLine);
            } else {
                validTaxLine.addPrice(taxLine.getPrice());
            }
        }

        // làm tròn
        return validTaxLines.values().stream()
                .peek(taxLine -> {
                    var roundedPrice = taxLine.getPrice().setScale(currency.getDefaultFractionDigits(), RoundingMode.UP);
                    taxLine.setPrice(roundedPrice);
                }).toList();
    }

    private TrackingInfo buildTrackingInfo(String source, String sourceName, OrderCreateRequest orderRequest, String reference) {
        return TrackingInfo.builder()
                .source(source)
                .sourceName(sourceName)
                .cartToken(orderRequest.getCartToken())
                .checkoutToken(orderRequest.getCheckoutToken())
                .landingSite(orderRequest.getLandingSite())
                .reference(reference)
                .sourceIdentifier(orderRequest.getSourceIdentifier() == null ? orderRequest.getReference() : orderRequest.getSourceIdentifier())
                .sourceUrl(orderRequest.getSourceUrl())
                .build();
    }

    private String buildReference(int storeId, OrderCreateRequest orderRequest) {
        var reference = orderRequest.getReference();
        if (StringUtils.isNotBlank(reference)) {
            var orderDto = orderDao.getByReference(storeId, reference);
            if (orderDto != null)
                reference = UUID.randomUUID().toString().replace("-", "");
        }
        return reference;
    }

    private Pair<String, String> mapSource(UUID clientId, String source, String sourceName) {
        if (clientId != null) {
            var clientIdString = clientId.toString();
            if (StringUtils.isBlank(sourceName)) {
                return Pair.of(clientIdString, clientIdString);
            } else if (StringUtils.isBlank(source)) {
                return Pair.of(clientIdString, sourceName);
            }
        } else {
            if (StringUtils.isBlank(sourceName)) {
                sourceName = "web";
            }
        }
        return Pair.of(source, sourceName);
    }

    private StoreDto getStoreById(Integer storeId) {
        var store = storeDao.findById(storeId);
        if (store != null) return store;
        throw new NotFoundException("store not found");
    }

    private Currency resolveCurrency(String currencyCodeRequest, Currency storeCurrency) {
        if (StringUtils.isNotBlank(currencyCodeRequest)) {
            if (!Order.DEFAUT_CURRENCY.getCurrencyCode().equals(currencyCodeRequest)) {
                this.validateCurrency(currencyCodeRequest);
                return Currency.getInstance(currencyCodeRequest);
            }
            return Order.DEFAUT_CURRENCY;
        }
        return storeCurrency == null ? Order.DEFAUT_CURRENCY : storeCurrency;
    }

    public static void validateCurrency(String currencyCode) {
        var currency = SupportCurrencies.getByCode(currencyCode);
        if (currency == null) {
            throw new ConstrainViolationException("currency", "not supported");
        }
    }

    @Transactional
    public int createRefund(OrderId orderId, RefundRequest refundRequest) {
        var store = getStoreById(orderId.getStoreId());
        var order = findOrderById(orderId);

        var refundResult = addRefund(order, refundRequest);
        return 0;
    }

    private RefundResult addRefund(Order order, RefundRequest refundRequest) {
        var refundResult = buildRefund(order, refundRequest);
        return null;
    }

    private RefundResult buildRefund(Order order, RefundRequest refundRequest) {
        var suggestedRefund = calculationService.calculateRefund(order, refundRequest);
        var refundLineItems = buildRefundLineItems(suggestedRefund);

        var refundTransaction = buildRefundTransaction(
                suggestedRefund, order, refundRequest.getTransactions());

        var orderAdjustments = buildOrderAdjustment(
                suggestedRefund, refundLineItems, refundTransaction, order);
        return null;
    }

    private Set<OrderAdjustment> buildOrderAdjustment(RefundCalculateResponse suggestedRefund, Set<RefundLineItem> refundLineItems, List<TransactionCreateRequest> refundTransaction, Order order) {
        return new LinkedHashSet<>();
    }

    private List<TransactionCreateRequest> buildRefundTransaction(
            RefundCalculateResponse suggestedRefund,
            Order order,
            List<TransactionCreateRequest> inputTransactions) {
        if (CollectionUtils.isEmpty(inputTransactions)) {
            return List.of();
        }
        var refundTransactionRequests = inputTransactions.stream()
                .filter(request -> {
                    if (!NumberUtils.isPositive(request.getAmount())) return false;
                    request.setKind(OrderTransaction.Kind.refund);
                    request.setSourceName("");
                    request.setStatus(OrderTransaction.Status.success);
                    return true;
                })
                .toList();
        if (refundTransactionRequests.isEmpty()) {
            return List.of();
        }

        var orderTransactions = transactionRepository.findByOrderId(order.getId());
        for (var refundTransaction : refundTransactionRequests) {
            var valid = true;
            if (!NumberUtils.isPositive(refundTransaction.getParentId())) {
                valid = false;
            } else {
                valid = orderTransactions.stream()
                        .anyMatch(tran -> tran.getId().getId() == refundTransaction.getParentId());
            }
            if (!valid) {
                throw new ConstrainViolationException(
                        "transactions",
                        "12"
                );
            }
        }

        var requestedAmount = refundTransactionRequests.stream()
                .map(TransactionCreateRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return refundTransactionRequests;
    }

    private Set<RefundLineItem> buildRefundLineItems(RefundCalculateResponse suggestedRefund) {
        var suggestedRefundLineItems = suggestedRefund.getRefundItems();
        if (CollectionUtils.isEmpty(suggestedRefundLineItems)) {
            return new LinkedHashSet<>();
        }
        var ids = orderIdGenerator.generateRefundLineIds(suggestedRefundLineItems.size());
        return suggestedRefundLineItems.stream()
                .map(line -> new RefundLineItem(ids.removeFirst(), line))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    record RefundResult(Refund refund, List<TransactionCreateRequest> transactions) {
    }

    private Order findOrderById(OrderId orderId) {
        var order = orderRepository.findById(orderId);
        if (order == null) throw new NotFoundException("order not found by id = " + orderId.toString());
        if (order.getClosedOn() != null) throw new ConstrainViolationException("order ", ":");
        return order;
    }
}
