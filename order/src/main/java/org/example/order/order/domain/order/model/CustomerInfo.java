package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Getter
@Builder
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CustomerInfo {

    @Email
    @Size(max = 128)
    private String email;

    @Size(max = 21)
    private String phone;

    @Min(1)
    private Integer customerId;

    private boolean buyerAcceptMarketing; // khách hàng có đồng ý tiếp thị hay không
}
