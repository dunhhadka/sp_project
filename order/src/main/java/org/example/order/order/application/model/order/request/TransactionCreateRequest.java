package org.example.order.order.application.model.order.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.model.PaymentInfo;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class TransactionCreateRequest {

    private Integer orderId;
    private Integer locationId;
    private @Size(max = 250) String gateway;
    private @Size(max = 20) String processingMethod;

    private @Size(max = 50) String authorization;
    private @Size(max = 500) String message;

    private @Size(max = 50) String sourceName;
    private @Size(max = 50) String deviceId;
    private @Min(0) Integer parentId;

    private @NotNull OrderTransaction.Kind kind;
    private OrderTransaction.Status status;

    private @Min(0) BigDecimal amount;

    private @Size(max = 3) String currency;

    private @Size(max = 50) Map<@NotBlank @Size(max = 50) String, @Size(max = 255) String> paymentDetails;

    private @Size(max = 50) Map<@NotBlank @Size(max = 50) String, Object> receipt;

    private PaymentInfo paymentInfo;

    private Instant processedOn;

    private OrderTransaction.CauseType causeType;

    @JsonIgnore
    private int tempId;
}
