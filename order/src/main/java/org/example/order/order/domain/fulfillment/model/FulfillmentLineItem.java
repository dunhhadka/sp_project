package org.example.order.order.domain.fulfillment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import java.time.Instant;

@Getter
@Entity
@Table(name = "fulfillment_line_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FulfillmentLineItem {

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "fulfillmentId", referencedColumnName = "id")
    private Fulfillment aggRoot;

    @Id
    private int id;
    private Integer orderId;
    private Integer lineItemId;
    @Min(1)
    private int quantity;
    private int effectiveQuantity;

    @Column(columnDefinition = "DATETIME2")
    private Instant createdOn;

    @Version
    private Integer version;
}
