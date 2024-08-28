package org.example.order.order.domain.transaction.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInfo {
    private Long paymentId;
    private Long paymentMethodId;
    private String paymentMethodName;
    private Integer providerId;
    @Size(max = 50)
    private String paymentBillNumber;
    @Size(max = 128)
    private String reference;
}
