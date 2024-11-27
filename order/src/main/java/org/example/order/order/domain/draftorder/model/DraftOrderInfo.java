package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.ddd.ValueObject;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Currency;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DraftOrderInfo extends ValueObject<DraftOrderInfo> {
    @NotNull
    private Currency currency;
    @Size(max = 2000)
    private String note;
    @Email
    private String email;
    private String phone;

    @Size(max = 2000)
    private String sourceName;
    private Boolean taxExempt; // ngoại trừ tax
    private Integer locationId;
    private Integer assigneeId;
}
