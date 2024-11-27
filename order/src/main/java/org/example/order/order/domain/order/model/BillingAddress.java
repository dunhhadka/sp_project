package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;

@Entity
@Getter
@Table(name = "billing_address")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingAddress {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order aggRoot;

    @Id
    private int id;

    @JsonUnwrapped
    @Embedded
    private @Valid MailingAddress addressInfo;

    public BillingAddress(int id, MailingAddress addressInfo) {
        this.id = id;
        this.addressInfo = addressInfo;
    }
}
