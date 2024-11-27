package org.example.order.order.application.model.draftorder.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class DraftOrderRequest {
    private Integer userId;
    private @Size(max = 2000) String note;
    private @Size(max = 3) String currency;
    private @Size(max = 50) List<@Size(max = 250) String> tags;
    private @Email String email;
    private String phone;
    private Integer locationId;
    private Integer copyOrderId;
    private Boolean useCustomerDefaultAddress;
    private Boolean taxExempt;

    private @Valid DraftOrderAddressRequest billingAddress;
    private @Valid DraftOrderAddressRequest shippingAddress;
    private @Valid DraftAppliedDiscountRequest appliedDiscount;
    private @Valid DraftShippingLineRequest shippingLine;
    private @Valid List<DraftPropertyRequest> noteAttributes;

    private DraftCustomerRequest customer;
}
