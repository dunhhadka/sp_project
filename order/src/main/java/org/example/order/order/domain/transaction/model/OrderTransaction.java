package org.example.order.order.domain.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.ddd.AggregateRoot;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Map;

@Getter
@Entity
@Table(name = "order_transaction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderTransaction extends AggregateRoot<OrderTransaction> {

    @EmbeddedId
    @JsonUnwrapped
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "id", referencedColumnName = "id")
    private TransactionId id;

    private Integer orderId;

    private Integer refundId;

    private Integer parentId;

    private Integer locationId;

    @Valid
    @Embedded
    @JsonUnwrapped
    private ClientInfo clientInfo;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    @Size(max = 3)
    private Currency currency;

    @NotNull
    @Convert(converter = Kind.ValueConverter.class)
    private Kind kind;

    @Enumerated(value = EnumType.STRING)
    private Status status;

    @Size(max = 255)
    private String message;

    @Valid
    @Embedded
    private PaymentInfo paymentInfo;

    @Size(max = 50)
    @Column(name = "payment_details")
    @Convert(converter = StringMapAttributeConverter.class)
    private Map<@NotBlank @Size(max = 50) String, @Size(max = 255) String> paymentDetails;

    @Size(max = 50)
    @Convert(converter = MapObjectAttributeConverter.class)
    private Map<String, Object> receipt;

    @Column(columnDefinition = "DATETIME2")
    private Instant processAt;
    @Column(columnDefinition = "DATETIME2")
    private Instant createdOn;

    @Size(max = 250)
    private String gateway;

    public boolean isCaptureOrSaleSuccess() {
        return true;
    }

    @JsonIgnore
    public boolean isRefundSuccess() {
        return Kind.refund.equals(this.kind) && Status.success.equals(this.status);
    }

    public enum Kind implements CustomValueEnum<String> {
        sale("sale"),
        authorization("authorization"),
        capture("capture"),
        refund("refund"),
        _void("void");

        private final String value;

        Kind(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Converter
        public static class ValueConverter
                extends AbstractEnumConverter<Kind>
                implements AttributeConverter<Kind, String> {

            public ValueConverter() {
                super(Kind.class);
            }
        }
    }

    public enum Status {
        pending,
        success,
        failure,
        error
    }

    public enum CauseType {
        customer_checkout, manual, voucher_allocation, cod_transfer, external
    }
}

