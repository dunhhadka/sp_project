package org.example.order.order.application.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.exception.UserError;
import org.example.order.order.application.model.order.context.OrderCreatedEvent;
import org.example.order.order.application.model.order.request.TransactionCreateRequest;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.PaymentMethodInfo;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.model.OrderTransactionIdGenerator;
import org.example.order.order.domain.transaction.model.PaymentInfo;
import org.example.order.order.domain.transaction.model.TransactionId;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.example.order.order.application.service.transaction.TransactionHelper.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionWriteService {

    private final OrderRepository orderRepository;
    private final OrderTransactionIdGenerator transactionIdGenerator;

    private final MessageSource messageSource;

    @EventListener(OrderCreatedEvent.class)
    public void handleOrderTransactionAdded(OrderCreatedEvent event) {
        var paymentResult = event.getPaymentResult();
        if (paymentResult.isFromCheckout()) {
            return;
        }

        var store = event.getStore();
        var orderId = event.getOrderId();
        var transactionRequests = event.getTransactionRequests();

        if (CollectionUtils.isEmpty(transactionRequests)) {
            return;
        }

        Map<Integer, OrderTransaction> transactionMap = new HashMap<>();

        var order = orderRepository.findById(orderId);

        var orderGateWay = getRepresentativeGateway(order);

        var paymentMethodIds = transactionRequests.stream()
                .map(TransactionCreateRequest::getPaymentInfo)
                .filter(Objects::nonNull)
                .map(PaymentInfo::getPaymentId)
                .distinct()
                .toList();

        var requestedGatewayNames = transactionRequests.stream()
                .map(TransactionCreateRequest::getGateway)
                .filter(StringUtils::isNotBlank)
                .toList();

        var paymentMethodMap = verifyPaymentMethods(store.getId(), paymentMethodIds, requestedGatewayNames);

        var transactionIdQueue = transactionIdGenerator.generateOrderTransactionIds(transactionRequests.size());

        transactionRequests.forEach(req -> req.setTempId(transactionIdQueue.removeFirst()));

        transactionRequests.forEach(txnReq -> {
            var requestedKind = txnReq.getKind();
            var transactionId = new TransactionId(store.getId(), txnReq.getTempId());
            var locationId = order.getLocationId();
            Integer userId = null;
            String clientId = null;
            var deviceId = txnReq.getDeviceId();
            var sourceName = determineSourceName(txnReq.getSourceName(), order.getTrackingInfo().getSourceName(), clientId);
            var processingMethod = txnReq.getProcessingMethod();
            var amount = OrderTransaction.Kind._void.equals(txnReq.getKind()) ? BigDecimal.ZERO : txnReq.getAmount();
            var currencyCode = order.getMoneyInfo().getCurrency().getCurrencyCode();
            var authorization = txnReq.getAuthorization();
            var receipt = txnReq.getReceipt();
            var processAt = txnReq.getProcessedOn();

            var parentTransaction = determineParentTransaction(txnReq, transactionRequests);
            var parentId = Optional.ofNullable(parentTransaction).map(TransactionCreateRequest::getTempId).orElse(null);

            var paymentInfo = determinePaymentInfo(txnReq.getPaymentInfo(), txnReq.getGateway(), Optional.ofNullable(parentTransaction).map(TransactionCreateRequest::getPaymentInfo).orElse(null), paymentMethodMap);
        });
    }

    private Map<Integer, PaymentMethod> verifyPaymentMethods(int storeId, List<Long> paymentMethodIds, List<String> paymentGateways) {
        List<PaymentMethod> paymentMethods = null;

        if (CollectionUtils.isNotEmpty(paymentMethodIds)) {
            if (paymentMethodIds.size() == 1) {
                var paymentMethod = new PaymentMethod();
                if ("active".equals(paymentMethod.getStatus())) {
                    throw new ConstrainViolationException(UserError.builder()
                            .code("not_active")
                            .fields(List.of("payment_method"))
                            .message(messageSource.getMessage("payment.error.payment", null, LocaleContextHolder.getLocale()))
                            .build());
                }
                paymentMethods = List.of(paymentMethod);
            } else {
                paymentMethods = new ArrayList<>();
            }

            var resultPaymentMethodIds = paymentMethods.stream()
                    .map(PaymentMethod::getId)
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());

            var notFoundPaymentMethodIds = paymentMethodIds.stream()
                    .filter(id -> !resultPaymentMethodIds.contains(id))
                    .toList();
            if (CollectionUtils.isNotEmpty(notFoundPaymentMethodIds)) {
                throw new NotFoundException("Payment methods not found: " + notFoundPaymentMethodIds);
            }
        } else if (CollectionUtils.isNotEmpty(paymentGateways)) {
            paymentMethods = new ArrayList<>();
        }

        return Optional.ofNullable(paymentMethods)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(
                        PaymentMethod::getId,
                        Function.identity(), (a, b) -> a));
    }

    private String getRepresentativeGateway(Order order) {
        return Optional.ofNullable(order.getPaymentMethodInfo())
                .map(PaymentMethodInfo::getGateWay)
                .flatMap(gw -> Arrays.stream(gw.trim().split(",")).findFirst())
                .orElse(null);
    }

}
