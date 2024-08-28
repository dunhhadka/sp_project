package org.example.order.order.application.model.order.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.refund.model.RefundLineItem;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class RefundRequest {
    @JsonAlias("processed_at")
    @JsonProperty("processed_at")
    private Instant processAt;

    private Cancel cancel;

    private Shipping shipping;

    private List<@Valid LineItem> refundLineItems;

    private BigDecimal refundValue;

    private boolean restock;

    private @Size(max = 1000) String note;

    private List<@Valid TransactionCreateRequest> transactions;

    private boolean sendNotification;

    private Option option;

    @Getter
    @Setter
    public static class Cancel {
        private boolean refundFullAmount = true;
        private boolean refundShipping = true;
    }

    @Getter
    @Setter
    public static class Shipping {
        private Boolean fullRefund;
        @PositiveOrZero
        private BigDecimal amount;
    }

    @Getter
    @Setter
    @Builder(toBuilder = true)
    public static class LineItem {
        private @Min(1) int lineItemId;
        private int quantity;
        private Long locationId;
        private boolean removal;
        private RefundLineItem.RestockType restockType;

        @JsonIgnore
        public boolean isRestock() {
            return restockType != null && restockType != RefundLineItem.RestockType.no_restock;
        }
    }

    @JsonIgnore
    public Option getOption() {
        return this.option == null ? Option.DEFAULT : this.option;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Option {
        public static Option DEFAULT = new Option(false, false, true, false);

        // old c# admin
        private boolean legacy;

        // legacy flag
        private boolean restock;

        // create/calculate
        private boolean create;

        private boolean editOrder;

        public static Option forCalculateRefund(boolean legacy, boolean restock) {
            return new Option(legacy, restock, false, false);
        }

        public static Option forCreateRefund(boolean legacy, boolean restock) {
            return new Option(legacy, restock, true, false);
        }

        public static Option forEditOrder() {
            return new Option(false, false, true, true);
        }
    }
}
