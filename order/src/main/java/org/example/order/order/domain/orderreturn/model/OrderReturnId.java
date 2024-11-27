package org.example.order.order.domain.orderreturn.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class OrderReturnId implements Serializable {
    private int storeId;
    private int id;
}
