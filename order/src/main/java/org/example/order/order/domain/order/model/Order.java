package org.example.order.order.domain.order.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.utils.OrderHelper;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor
public class Order extends AggregateRoot<Order> {

    public static final Locale DEFAULT_LOCAL = new Locale.Builder().setLanguage("vi").setRegion("VN").build();
    public static final Currency DEFAUT_CURRENCY = Currency.getInstance(DEFAULT_LOCAL);
    public static final Instant MIN_PROCESSED_ON = Instant.ofEpochSecond(1420070400);

    @Setter
    @Transient
    @JsonIgnore
    private OrderIdGenerator idGenerator;

    @EmbeddedId
    @JsonUnwrapped
    @AttributeOverride(name = "storeId", column = @Column(name = "storeId"))
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    private OrderId id;

    private Integer locationId;

    @Column(columnDefinition = "DATETIME2")
    private Instant createdOn;

    @NotNull
    @Column(columnDefinition = "DATETIME2")
    private Instant modifiedOn;

    @Column(columnDefinition = "DATETIME2")
    private Instant processOn;

    @Column(columnDefinition = "DATETIME2")
    private Instant closedOn;

    @Column(columnDefinition = "DATETIME2")
    private Instant cancelledOn;

    @Enumerated(value = EnumType.STRING)
    private OrderStatus status;

    @Enumerated(value = EnumType.STRING)
    private FinancialStatus financialStatus;

    @Enumerated(value = EnumType.STRING)
    private FulfillmentStatus fulfillmentStatus;

    @Enumerated(value = EnumType.STRING)
    private ReturnStatus returnStatus;

    @Transient
    private boolean restock;

    @Min(0)
    private int totalWeight;

    @Size(max = 2000)
    private String note;

    @Embedded
    @JsonUnwrapped
    private @Valid ReferenceInfo referenceInfo;

    @Embedded
    @JsonUnwrapped
    private @Valid TrackingInfo trackingInfo;

    @Embedded
    @JsonUnwrapped
    private @Valid CustomerInfo customerInfo;

    @Embedded
    @JsonUnwrapped
    private @Valid MoneyInfo moneyInfo;

    @Embedded
    @JsonUnwrapped
    private @Valid PaymentMethodInfo paymentMethodInfo;

    @Size(max = 100)
    @ElementCollection
    @CollectionTable(name = "order_tags", joinColumns = {
            @JoinColumn(name = "orderId", referencedColumnName = "id"),
            @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    })
    private List<@Valid OrderTag> tags = new ArrayList<>();

    @Size(max = 100)
    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid LineItem> lineItems = new ArrayList<>();

    @Size(max = 100)
    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid ShippingLine> shippingLines = new ArrayList<>();

    @Size(max = 10)
    @OneToMany(mappedBy = "aggRoot", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid OrderDiscountCode> discountCodes = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id asc")
    private List<@Valid DiscountApplication> discountApplications = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<BillingAddress> billingAddresses;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<ShippingAddress> shippingAddresses;

    @Size(max = 100)
    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<CombinationLine> combinationLines = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id desc")
    private Set<Refund> refunds = new LinkedHashSet<>();

    private boolean taxExempt; // đơn hàng không áp dụng thuế
    private boolean taxIncluded; // đơn hàng đã bao gồm thuế

    @Version
    private Integer version;

    public Order(
            int storeId,
            Instant processOn,
            CustomerInfo customerInfo,
            TrackingInfo trackingInfo,
            Currency currency,
            int totalWeight,
            String note,
            List<String> tags,
            BillingAddress billingAddress,
            ShippingAddress shippingAddress,
            List<LineItem> lineItems,
            List<ShippingLine> shippingLines,
            List<OrderDiscountCode> discountCodes,
            List<DiscountAllocation> discountAllocations,
            List<DiscountApplication> discountApplications,
            boolean taxExempt,
            boolean taxesIncluded,
            String processingMethod,
            String gateWay,
            OrderIdGenerator orderIdGenerator,
            Long locationId,
            List<CombinationLine> combinationLines
    ) {
        this.generateId(storeId, orderIdGenerator);

        this.internalSetCustomerInfo(customerInfo);
        this.internalSetTrackingInfo(trackingInfo);

        this.note = note;
        this.mergeTags(tags);

        this.internalSetBillingAddress(billingAddress);
        this.internalSetShippingAddress(shippingAddress);

        this.moneyInfo = MoneyInfo.builder().currency(currency).build();

        this.privateSetCombinationLines(combinationLines);
        this.privateSetLineItems(lineItems);
        this.privateSetShippingLines(shippingLines);
        this.privateSetDiscountCodes(discountCodes);

        this.taxExempt = taxExempt;
        this.taxIncluded = taxesIncluded;

        this.allocateDiscounts(discountApplications, discountAllocations);
        this.totalWeight = calculateTotalWeight(totalWeight);
        this.status = OrderStatus.open;
        this.returnStatus = ReturnStatus.no_return;
        this.paymentMethodInfo = new PaymentMethodInfo(gateWay, processingMethod);

        this.calculateMoneyFoInsert();
        this.initFinancialStatus();

        this.locationId = locationId == null ? null : locationId.intValue();

        this.referenceInfo = new ReferenceInfo();

        this.createdOn = this.modifiedOn = Instant.now();
        this.setProcessOn(processOn, createdOn);
    }

    private void setProcessOn(Instant processOn, Instant defaultValue) {
        this.processOn = processOn;
        if (this.processOn.isBefore(MIN_PROCESSED_ON) || this.processOn.isAfter(defaultValue)) {
            throw new ConstrainViolationException("process_on", "invalid");
        }
    }

    private void initFinancialStatus() {
        if (this.moneyInfo.getTotalPrice().compareTo(BigDecimal.ZERO) == 0) {
            this.changeFinancialStatus(FinancialStatus.paid);
        } else {
            this.changeFinancialStatus(FinancialStatus.pending);
        }
    }

    private void changeFinancialStatus(FinancialStatus financialStatus) {
        if (Objects.equals(this.financialStatus, financialStatus)) return;
        this.financialStatus = financialStatus;
    }

    private void calculateMoneyFoInsert() {
        BigDecimal lineItemDiscountedTotal = this.lineItems.stream()
                .map(LineItem::getDiscountedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productDiscount = this.lineItems.stream()
                .map(LineItem::getProductDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal orderDiscount = this.lineItems.stream()
                .map(LineItem::getOrderDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLineItemTax = this.lineItems.stream()
                .map(LineItem::getTotalTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalShippingPrice = BigDecimal.ZERO;
        BigDecimal shippingDiscount = BigDecimal.ZERO;
        BigDecimal totalShippingTax = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(this.shippingLines)) {
            totalShippingPrice = this.shippingLines.stream()
                    .map(ShippingLine::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            shippingDiscount = this.shippingLines.stream()
                    .filter(s -> !CollectionUtils.isEmpty(s.getDiscountAllocations()))
                    .flatMap(s -> s.getDiscountAllocations().stream().map(DiscountAllocation::getAmount))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalShippingTax = this.shippingLines.stream()
                    .filter(s -> !CollectionUtils.isEmpty(s.getTaxLines()))
                    .flatMap(s -> s.getTaxLines().stream().map(TaxLine::getPrice))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal orderCartDiscount = orderDiscount.add(shippingDiscount);
        BigDecimal totalDiscount = orderCartDiscount.add(productDiscount);

        BigDecimal totalTax = totalLineItemTax.add(totalShippingTax);

        BigDecimal totalLineItemPrice = lineItemDiscountedTotal.add(productDiscount);
        BigDecimal subtotalPrice = lineItemDiscountedTotal;
        BigDecimal subtotalShippingPrice = totalShippingPrice.subtract(shippingDiscount);
        BigDecimal totalPrice = subtotalPrice.add(subtotalShippingPrice);

        if (!this.taxIncluded) {
            totalPrice = totalPrice.add(totalTax);
        }

        var moneyInfoBuilder = this.moneyInfo.toBuilder()
                .totalPrice(totalPrice)
                .subtotalPrice(subtotalPrice)
                .totalLineItemPrice(totalLineItemPrice)
                .cartDiscountAmount(orderCartDiscount)
                .totalDiscount(totalDiscount)
                .totalTax(totalTax)
                .currentTotalPrice(totalPrice)
                .currentSubtotalPrice(subtotalPrice)
                .currentCartDiscountAmount(orderCartDiscount)
                .currentTotalDiscount(totalDiscount)
                .currentTotalTax(totalTax);

        moneyInfoBuilder
                .unpaidAmount(totalPrice)
                .totalOutstanding(totalPrice);

        moneyInfoBuilder
                .originalTotalPrice(totalPrice);

        this.moneyInfo = moneyInfoBuilder.build();
    }

    private int calculateTotalWeight(int input) {
        int result = input;
        if (result < 1) result = this.lineItems.stream().mapToInt(LineItem::getTotalWeight).sum();
        return result;
    }

    private void allocateDiscounts(List<DiscountApplication> discountApplications, List<DiscountAllocation> discountAllocations) {
        if (discountApplications == null || discountAllocations == null) return;

        this.discountApplications = discountApplications;
        for (var discountApplication : discountApplications) {
            discountApplication.setAggRoot(this);
        }
        for (var discountAllocation : discountAllocations) {
            discountAllocation.setRootId(this.getId());
            switch (discountAllocation.getTargetType()) {
                case line_item -> this.lineItems.stream()
                        .filter(l -> l.getId() == discountAllocation.getTargetId())
                        .findFirst().ifPresent(line ->
                                line.allocateDiscount(discountAllocation));
                case shipping_line -> this.shippingLines.stream()
                        .filter(line -> line.getId() == discountAllocation.getTargetId())
                        .findFirst().ifPresent(shippingLine -> {
                            shippingLine.allocateDiscount(discountAllocation);
                        });
            }
        }
    }

    private void privateSetDiscountCodes(List<OrderDiscountCode> discountCodes) {
        if (CollectionUtils.isEmpty(discountCodes)) return;

        this.discountCodes = discountCodes;
        for (var discountCode : this.discountCodes) {
            discountCode.setAggRoot(this);
        }
    }

    private void privateSetShippingLines(List<ShippingLine> shippingLines) {
        if (CollectionUtils.isEmpty(shippingLines)) return;

        this.shippingLines = shippingLines;
        for (var line : this.shippingLines) {
            line.setAggRoot(this);
            if (CollectionUtils.isEmpty(line.getTaxLines())) continue;
            for (var taxLine : line.getTaxLines()) {
                taxLine.setRootId(this.getId());
            }
        }
    }

    private void privateSetLineItems(List<LineItem> lineItems) {
        this.lineItems = lineItems;
        for (var lineItem : lineItems) {
            lineItem.setAggRoot(this);
            if (CollectionUtils.isEmpty(lineItem.getTaxLines())) continue;
            for (var taxLine : lineItem.getTaxLines()) {
                taxLine.setRootId(this.id);
            }
        }
    }

    private void privateSetCombinationLines(List<CombinationLine> combinationLines) {
        this.combinationLines = combinationLines;
        for (var combinationLine : combinationLines) {
            combinationLine.setAggRoot(this);
        }
    }

    private void internalSetShippingAddress(ShippingAddress shippingAddress) {
        if (shippingAddress == null) return;
        shippingAddress.setAggRoot(this);
        this.shippingAddresses = List.of(shippingAddress);
    }

    private void internalSetBillingAddress(BillingAddress billingAddress) {
        if (billingAddress == null) return;
        billingAddress.setAggRoot(this);
        this.billingAddresses = List.of(billingAddress);
    }

    private void mergeTags(List<String> newTagValues) {
        if (newTagValues == null) newTagValues = new ArrayList<>();

        var currentTagValues = this.tags.stream().map(OrderTag::getValue).toList();
        newTagValues = newTagValues.stream().sorted().toList();
        if (newTagValues.equals(currentTagValues)) return;

        for (var currentTag : this.tags) {
            if (newTagValues.contains(currentTag.getValue())) continue;
            internalRemoveTag(currentTag);
        }
        for (var tag : newTagValues) {
            internalAddTag(tag);
        }
    }

    private void internalAddTag(String tag) {
        if (this.tags.stream().anyMatch(t -> Objects.equals(t.getValue(), tag))) return;
        var newTag = new OrderTag(tag, tag);
        this.tags.add(newTag);
        this.modifiedOn = Instant.now();
    }

    private void internalRemoveTag(OrderTag tag) {
        if (!this.tags.contains(tag)) return;
        this.tags.remove(tag);
        this.modifiedOn = Instant.now();
    }

    private void internalSetTrackingInfo(TrackingInfo trackingInfo) {
        Objects.requireNonNull(trackingInfo);
        this.trackingInfo = trackingInfo;
    }

    private void internalSetCustomerInfo(CustomerInfo customerInfo) {
        Objects.requireNonNull(customerInfo);
        this.customerInfo = customerInfo;
    }

    private void generateId(int storeId, OrderIdGenerator orderIdGenerator) {
        this.idGenerator = orderIdGenerator;
        this.id = new OrderId(storeId, orderIdGenerator.generateOrderId());
    }

    public void markAsFulfilled() {
        var fulfilledLineItemMap = this.lineItems.stream()
                .filter(l -> l.getFulfillableQuantity() > 0)
                .collect(Collectors.toMap(LineItem::getId, LineItem::getFulfillableQuantity));
        this.updateFulfilledLineItems(fulfilledLineItemMap);
    }

    private void updateFulfilledLineItems(Map<Integer, Integer> fulfilledLineItemMap) {
        fulfilledLineItemMap.forEach((lineItemId, fulfilledQuantity) -> {
            var lineItem = this.lineItems.stream()
                    .filter(l -> l.getId() == lineItemId)
                    .findFirst().orElse(null);
            if (lineItem == null) return;
            updateFulfilledLineItem(lineItem, fulfilledQuantity);
        });

        updateFulfillmentStatus();
        this.modifiedOn = Instant.now();
    }

    private void updateFulfillmentStatus() {
        int unfulfilled = 0;
        int fulfilled = 0;
        int restocked = 0;
        int totalLineItem = this.lineItems.size();
        for (var line : this.lineItems) {
            if (line.getFulfillmentStatus() == null) {
                unfulfilled++;
                continue;
            }
            switch (line.getFulfillmentStatus()) {
                case fulfilled -> fulfilled++;
                case restocked -> restocked++;
            }
        }
        var orderFulfillStatus = FulfillmentStatus.partial;
        if (unfulfilled == totalLineItem) {
            orderFulfillStatus = null;
        } else if (fulfilled == totalLineItem) {
            orderFulfillStatus = FulfillmentStatus.fulfilled;
        } else if (restocked == totalLineItem) {
            orderFulfillStatus = FulfillmentStatus.restocked;
        }
        this.fulfillmentStatus = orderFulfillStatus;
    }

    private void updateFulfilledLineItem(LineItem lineItem, Integer fulfilledQuantity) {
        lineItem.markAsFulfilled(fulfilledQuantity);
        this.modifiedOn = Instant.now();
    }

    public void recognizeTransaction(TransactionInput transactionInput) {
        switch (transactionInput.getKind()) {
            case sale -> recognizeSaleTransaction(transactionInput);
            case refund -> recognizeRefundTransaction(transactionInput);
        }
    }

    private void recognizeRefundTransaction(TransactionInput transactionInput) {

    }

    private void recognizeSaleTransaction(TransactionInput transactionInput) {
        if (OrderTransaction.Status.success.equals(transactionInput.getStatus())) {
            var totalReceived = this.moneyInfo.getTotalReceived().add(transactionInput.getAmount());
            var netPay = this.moneyInfo.getNetPayment().add(transactionInput.getAmount());
            var unpaidAmount = this.moneyInfo.getUnpaidAmount().subtract(transactionInput.getAmount());
            var totalOutStanding = this.moneyInfo.getTotalOutstanding().subtract(transactionInput.getAmount());

            var changedMoneyInfo = this.moneyInfo.toBuilder()
                    .totalReceived(totalReceived)
                    .netPayment(netPay)
                    .unpaidAmount(unpaidAmount)
                    .totalOutstanding(totalOutStanding)
                    .build();

            this.changeMoneyInfo(changedMoneyInfo);
        }
    }

    private void changeMoneyInfo(MoneyInfo moneyInfo) {
        this.moneyInfo = moneyInfo;
    }

    public ShippingAddress getShippingAddress() {
        if (CollectionUtils.isEmpty(this.shippingAddresses)) return null;
        return this.getShippingAddresses().get(0);
    }

    public void recalculatePamentState(List<OrderTransaction> transactions) {
        this.moneyInfo = OrderHelper.recalculateMoneyInfo(this, transactions);
    }

    public void addRefund(Refund refund) {
        this.internalAddRefund(refund);
        this.updateRefundedLineItemStatus(refund.getRefundLineItems());
        this.recognizeRefund(refund);
        var refundTransaction = TransactionInput.builder()
                .kind(OrderTransaction.Kind.refund)
                .amount(refund.getTotalRefund())
                .build();
        this.updateFinancialStatus(refundTransaction);
    }

    private void updateFinancialStatus(TransactionInput refundTransaction) {
        var totalReceived = this.moneyInfo.getTotalReceived();
        if (totalReceived.compareTo(BigDecimal.ZERO) > 0) {
            var totalRefunded = this.moneyInfo.getTotalRefund();
            if (totalRefunded.compareTo(BigDecimal.ZERO) > 0) {

            }
        }
    }

    private void recognizeRefund(Refund refund) {
        // Tiền hoàn đã bao gồm khuyến mãi đơn hàng và sản phẩm
        var refundedProductSubtotal = refund.getLineItemSubtotalRefunded();
        // thuế hoàn của sản phẩm
        var refundedProductTax = refund.getTotalLineItemTaxRefunded();
        // Tiền giảm giá đơn hàng
        var refundedCartLevelDiscount = refund.getTotalCartDiscountRefunded();
        // tiền hoàn sản phẩm gốc không tính giảm giá
        var refundedProductOriginalPrice = refund.getRefundedOriginalPrice();
        // Tổng hoàn giảm giá sản phẩm và giảm giá đơn hàng
        var refundedDiscount = refundedProductOriginalPrice.subtract(refundedProductSubtotal);

        // Tổng hoàn của phí vận chuyển
        var refundedShipping = refund.getTotalShippingRefunded();
        // Tổng hoàn thuế vận chuyển
        var refundedShippingTax = refund.getTotalShippingTaxRefunded();
        //
        var refundedTax = refundedProductTax.add(refundedShippingTax);

        var currentTotalTax = this.moneyInfo.getTotalTax().subtract(refundedTax);

        var currentCartDiscountAmount = this.moneyInfo.getCurrentCartDiscountAmount().subtract(refundedCartLevelDiscount);
        var currentSubtotalPrice = this.moneyInfo.getCurrentSubtotalPrice().subtract(refundedProductSubtotal);
        var currentTotalDiscount = this.moneyInfo.getCurrentTotalDiscount().subtract(refundedDiscount);
        var currentTotalPrice = this.moneyInfo.getCurrentTotalPrice().subtract(refundedProductSubtotal);
        if (!this.isTaxIncluded()) {
            currentTotalPrice = currentTotalPrice.subtract(refundedTax);
        }
        var currentTotalPriceChange = this.moneyInfo.getCurrentTotalPrice().subtract(currentTotalPrice);

        var refundUnpaidDeduction = Optional.ofNullable(refund.getOutstandingAdjustmentAmount()).orElse(BigDecimal.ZERO);
        var refundedDiscrepancy = refund.getDiscrepancyAmount();

        var refundAmount = refund.getTotalRefunded();
        var totalRefunded = this.moneyInfo.getTotalRefund().add(refundAmount);

        var netPay = this.moneyInfo.getNetPayment().subtract(refundAmount);

        var unpaidAmountChanged = currentTotalPriceChange.subtract(refundAmount).add(refundUnpaidDeduction).subtract(refundedDiscrepancy);
        var unpaidAmount = this.moneyInfo.getUnpaidAmount().subtract(unpaidAmountChanged);

        var totalOutstanding = this.moneyInfo.getTotalOutstanding().subtract(unpaidAmountChanged);

        var changedMoneyInfo = this.moneyInfo.toBuilder()
                .currentTotalTax(currentTotalTax)
                .currentCartDiscountAmount(currentCartDiscountAmount)
                .currentSubtotalPrice(currentSubtotalPrice)
                .currentTotalPrice(currentTotalPrice)
                .netPayment(netPay)
                .unpaidAmount(unpaidAmount)
                .totalOutstanding(totalOutstanding)
                .build();

        this.changeMoneyInfo(changedMoneyInfo);
    }

    private void updateRefundedLineItemStatus(Set<RefundLineItem> refundLineItems) {
        var refundItemMap = new HashMap<Integer, List<RefundLineItem>>();
        for (var refundItem : refundLineItems) {
            var lineItemId = refundItem.getLineItemId();
            if (refundItemMap.containsKey(lineItemId)) {
                refundItemMap.get(lineItemId).add(refundItem);
            } else {
                var refundItemMapValue = new ArrayList<RefundLineItem>();
                refundItemMapValue.add(refundItem);
                refundItemMap.put(lineItemId, refundItemMapValue);
            }
        }
        for (var entry : refundItemMap.entrySet()) {
            this.lineItems.stream()
                    .filter(line -> line.getId() == entry.getKey())
                    .findFirst().ifPresent(lineItem -> lineItem.refund(entry.getValue()));
        }
    }

    private void internalAddRefund(Refund refund) {
        this.restock = refund.isRestock();
        if (this.refunds == null) this.refunds = new LinkedHashSet<>();
        this.refunds.add(refund);
        this.modifiedOn = Instant.now();
    }

    public BillingAddress getBillingAddress() {
        if (CollectionUtils.isEmpty(this.billingAddresses)) return null;
        return this.billingAddresses.get(0);
    }

    @Getter
    @Builder
    public static class TransactionInput {
        private Long id;
        private Long parentId;
        private String sourceName;
        private String gateway;
        private String authorization;
        private String errorCode;
        private BigDecimal amount;
        private OrderTransaction.Kind kind;
        private OrderTransaction.Status status;
    }

    public enum OrderStatus {
        open,
        closed,
        cancelled,
        deleted
    }

    public enum FinancialStatus {
        pending,
        authorized,
        partially_paid,
        paid,
        partially_refunded,
        refunded,
        voided
    }

    public enum FulfillmentStatus {
        fulfilled,
        partial,
        restocked
    }

    public enum ReturnStatus {
        in_process,
        no_return,
        returned
    }
}
