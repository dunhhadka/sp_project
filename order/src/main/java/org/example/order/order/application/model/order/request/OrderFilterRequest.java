package org.example.order.order.application.model.order.request;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Setter
public class OrderFilterRequest extends PagingFilterRequest {
    private List<Integer> ids;
    private String query;
    private String status;
    private String financialStatus;
    private String fulfillmentStatus;
    private String tag;
    private int customerId;
    private Instant createdOnMin;
    private Instant createdOnMax;
    private Instant modifiedOnMin;
    private Instant modifiedOnMax;
    private Instant processOnMin;
    private Instant processOnMax;

    public List<Integer> getIds() {
        return ids;
    }

    public String getQuery() {
        return query;
    }

    public String getStatus() {
        return status;
    }

    public String getFinancialStatus() {
        return financialStatus;
    }

    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }

    public String getTag() {
        return tag;
    }

    public int getCustomerId() {
        return customerId;
    }

    public Instant getCreatedOnMin() {
        return createdOnMin;
    }

    public Instant getCreatedOnMax() {
        return createdOnMax;
    }

    public Instant getModifiedOnMin() {
        return modifiedOnMin;
    }

    public Instant getModifiedOnMax() {
        return modifiedOnMax;
    }

    public Instant getProcessOnMin() {
        return processOnMin;
    }

    public Instant getProcessOnMax() {
        return processOnMax;
    }
}
