package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "draft_order_numbers")
public class DraftOrderNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int storeId;
    private int nextDraftOrderNumber;

    public DraftOrderNumber(int storeId) {
        this.storeId = storeId;
        this.nextDraftOrderNumber = 1;
    }

    public void update() {
        this.nextDraftOrderNumber++;
    }
}
