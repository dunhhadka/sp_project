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
@Table(name = "shipping_address")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    private Order aggRoot;

    @Id
    private int id;

    @JsonUnwrapped
    @Embedded
    private @Valid MailingAddress addressInfo;

    public ShippingAddress(int id, MailingAddress addressInfo) {
        this.id = id;
        this.addressInfo = addressInfo;
    }
}