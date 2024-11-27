package org.example.order.order.application.service.transaction;

import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.model.order.request.TransactionCreateRequest;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.model.PaymentInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TransactionHelper {
    public static String determineSourceName(String sourceName, String orderSourceName, String clientId) {
        return StringUtils.firstNonBlank(sourceName, orderSourceName, clientId, "web");
    }

    public static TransactionCreateRequest determineParentTransaction(
            TransactionCreateRequest txnReq,
            List<TransactionCreateRequest> transactionRequests
    ) {
        int indexFirstAuthorization = -1;
        int indexFirstSalePending = -1;
        int indexFirstSale = -1;
        int indexFirstCaptureSuccess = -1;

        for (int i = 0; i < transactionRequests.size(); i++) {
            var transactionCreateRequest = transactionRequests.get(i);

            if (indexFirstAuthorization == -1
                    && transactionCreateRequest.getKind() == OrderTransaction.Kind.authorization) {
                indexFirstAuthorization = i;
            }
            if (indexFirstSalePending == -1
                    && transactionCreateRequest.getKind() == OrderTransaction.Kind.sale
                    && transactionCreateRequest.getStatus() == OrderTransaction.Status.pending) {
                indexFirstSalePending = i;
            }
            if (indexFirstSale == -1 && transactionCreateRequest.getKind() == OrderTransaction.Kind.sale) {
                indexFirstSale = i;
            }
            if (indexFirstCaptureSuccess == -1
                    && OrderTransaction.Kind.capture.equals(transactionCreateRequest.getKind())
                    && OrderTransaction.Status.success.equals(transactionCreateRequest.getStatus())) {
                indexFirstCaptureSuccess = i;
            }

            if (indexFirstAuthorization != -1 && indexFirstSale != -1) {
                break;
            }
        }

        return null;
    }

    public static PaymentInfo determinePaymentInfo(
            PaymentInfo requestedPaymentInfo, String gateway,
            PaymentInfo parentPaymentInfo, Map<Integer, PaymentMethod> paymentMethodMap
    ) {
        PaymentInfo paymentInfo = null;
        if (requestedPaymentInfo != null && requestedPaymentInfo.getPaymentMethodId() != null) {
            var paymentMethod = paymentMethodMap.get(requestedPaymentInfo.getPaymentMethodId().intValue());
            if (paymentMethod != null) {
                paymentInfo = PaymentInfo.builder()
                        .paymentId(requestedPaymentInfo.getPaymentId())
                        .paymentMethodId(requestedPaymentInfo.getPaymentMethodId())
                        .paymentMethodName(requestedPaymentInfo.getPaymentMethodName())
                        .providerId(requestedPaymentInfo.getProviderId())
                        .build();
            }
        } else if (StringUtils.isNotBlank(gateway)) {
            var paymentMethod = paymentMethodMap.values().stream()
                    .filter(pm -> gateway.equals(pm.getName()))
                    .findFirst()
                    .orElse(null);
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

}
