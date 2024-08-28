package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import javax.validation.constraints.Size;

@Getter
@Builder
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentMethodInfo {
    @Size(max = 250)
    private String gateWay;
    @Size(max = 20)
    private String processingGateWay;
}
