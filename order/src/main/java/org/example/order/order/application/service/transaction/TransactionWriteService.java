package org.example.order.order.application.service.transaction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.model.order.context.OrderCreatedEvent;
import org.example.order.order.application.model.order.request.TransactionCreateRequest;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.PaymentMethodInfo;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.model.OrderTransactionIdGenerator;
import org.example.order.order.domain.transaction.model.PaymentInfo;
import org.example.order.order.domain.transaction.model.TransactionId;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionWriteService {

    private final OrderRepository orderRepository;
    private final OrderTransactionIdGenerator transactionIdGenerator;

    @EventListener(OrderCreatedEvent.class)
    public void handleOrderTransactionAdded(OrderCreatedEvent event) {
        log.debug("handle order transaction added: {}", event);

        var paymentResult = event.getOrderPaymentResult();
        if (paymentResult.isFromCheckout()) return;

        var store = event.getStore();
        var orderId = event.getOrderId();
        var transactionRequests = event.getTransactionRequests();

        Map<Long, OrderTransaction> transactionMap = new HashMap<>();

        var order = orderRepository.findById(orderId);

        var orderGateWay = getPaymentGateway(order);

        var paymentMethodIds = transactionRequests.stream()
                .map(TransactionCreateRequest::getPaymentInfo)
                .filter(Objects::nonNull)
                .map(PaymentInfo::getPaymentId)
                .toList();

        var requestedGatewayNames = transactionRequests.stream()
                .map(TransactionCreateRequest::getGateway)
                .filter(StringUtils::isNotBlank)
                .toList();

        var paymentMethodMap = verifyPaymentMethods(store.getId(), paymentMethodIds, requestedGatewayNames);

        var transactionIdsQueue = transactionIdGenerator.generateOrderTransactionIds(transactionRequests.size());

        transactionRequests.forEach(txnReq -> txnReq.setTempId(transactionIdsQueue.removeFirst()));

        transactionRequests.forEach(txnReq -> {
            var requestKind = txnReq.getKind();
            var transactionId = new TransactionId(store.getId(), txnReq.getTempId());
            var locationId = order.getLocationId();
            var deviceId = txnReq.getDeviceId();
            var sourceName = determineSourceName(txnReq.getSourceName(), order.getTrackingInfo().getSourceName());
            var processingMethod = txnReq.getProcessingMethod();
            var amount = OrderTransaction.Kind._void.equals(requestKind) ? BigDecimal.ZERO : txnReq.getAmount();
            var currencyCode = order.getMoneyInfo().getCurrency().getCurrencyCode();
            var receipt = txnReq.getReceipt();
            var causeType = determineCauseType(txnReq);
            var parentTransaction = determineParentTransaction(txnReq, transactionRequests);
            var parentId = Optional.ofNullable(parentTransaction).map(TransactionCreateRequest::getTempId).orElse(null);
            var paymentInfo = determinePaymentInfo(
                    txnReq.getPaymentInfo(),
                    txnReq.getGateway(),
                    Optional.ofNullable(parentTransaction).map(TransactionCreateRequest::getPaymentInfo).orElse(null),
                    paymentMethodMap);
            var gateWay = determineGateway(paymentInfo, StringUtils.firstNonBlank(txnReq.getGateway(), orderGateWay));
        });
    }

    private String determineGateway(PaymentInfo paymentInfo, String orderGateway) {
        return StringUtils.firstNonBlank(
                Optional.ofNullable(paymentInfo).map(PaymentInfo::getPaymentMethodName).orElse(null),
                orderGateway
        );
    }

    private PaymentInfo determinePaymentInfo(
            PaymentInfo paymentInfoRequest,
            String gateway,
            PaymentInfo parentPaymentInfo,
            Map<Long, PaymentMethod> paymentMethodMap
    ) {
        PaymentInfo paymentInfo = null;
        if (paymentInfoRequest != null && paymentInfoRequest.getPaymentMethodId() != null) {
            var paymentMethod = paymentMethodMap.get(paymentInfoRequest.getPaymentMethodId());
            paymentInfo = PaymentInfo.builder()
                    .paymentId(paymentInfoRequest.getPaymentId())
                    .paymentMethodId((long) paymentMethod.getId())
                    .paymentMethodName(paymentMethod.getName())
                    .providerId(paymentMethod.getProviderId())
                    .build();
        } else if (StringUtils.isNotBlank(gateway)) {
            var paymentMethod = paymentMethodMap.values().stream()
                    .filter(p -> StringUtils.equals(p.getName(), gateway))
                    .findFirst().orElse(null);
            if (paymentMethod != null) {
                paymentInfo = PaymentInfo.builder()
                        .paymentMethodId((long) paymentMethod.getId())
                        .paymentMethodName(paymentMethod.getName())
                        .providerId(paymentMethod.getProviderId())
                        .build();
            }
        }

        return Optional.ofNullable(paymentInfo).orElse(parentPaymentInfo);
    }

    private TransactionCreateRequest determineParentTransaction(
            TransactionCreateRequest transaction,
            List<TransactionCreateRequest> transactionRequests
    ) {
        return null;
    }

    private OrderTransaction.CauseType determineCauseType(TransactionCreateRequest transactionCreateRequest) {
        if (transactionCreateRequest.getCauseType() == null) {
            return OrderTransaction.CauseType.external;
        }
        return transactionCreateRequest.getCauseType();
    }

    private String determineSourceName(String sourceName, String orderSourceName) {
        return StringUtils.firstNonBlank(sourceName, orderSourceName, "web");
    }

    private Map<Long, PaymentMethod> verifyPaymentMethods(int id, List<Long> paymentMethodIds, List<String> requestedGatewayNames) {
        List<PaymentMethod> paymentMethods = new ArrayList<>();

        return new HashMap<>();
    }

    private String getPaymentGateway(Order order) {
        return Optional.ofNullable(order.getPaymentMethodInfo())
                .map(PaymentMethodInfo::getGateWay)
                .flatMap(gw -> Arrays.stream(gw.trim().split(",")).findFirst())
                .orElse(null);
    }

    @Getter
    @Setter
    public static class PaymentMethod {
        private int id;
        private int providerId;
        private String name;
        private String description;
    }
}
