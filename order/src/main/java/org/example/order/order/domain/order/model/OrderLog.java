package org.example.order.order.domain.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.ddd.AppEventType;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "order_logs")
public class OrderLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int storeId;
    private int orderId;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private AppEventType verb;

    @Lob
    private String properties;

    @Lob
    private String data;

    public OrderLog(int storeId, int orderId, AppEventType verb, String data) {
        this.storeId = storeId;
        this.orderId = orderId;
        this.verb = verb;
        this.data = data;
    }
}
