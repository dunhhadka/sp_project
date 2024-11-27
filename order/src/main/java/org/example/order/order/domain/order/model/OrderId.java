package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class OrderId implements Serializable {
    private int storeId;
    private int id;

    @Override
    public String toString() {
        return "OrderId{" +
                "id=" + id +
                ", storeId=" + storeId +
                '}';
    }
}
