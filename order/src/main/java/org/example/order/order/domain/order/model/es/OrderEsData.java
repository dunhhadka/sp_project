package org.example.order.order.domain.order.model.es;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.order.infrastructure.data.dto.AddressDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderEsData {
    private int id;
    private int storeId;

    // referenceInfo
    private int number;
    private int orderNumber;
    private String name;
    private String token;

    // Time info
    private Instant createdOn;
    private Instant modifiedOn;
    private Instant processOn;
    private Instant closedOn;
    private Instant cancelledOn;

    // money info
    private BigDecimal totalPrice;

    // payment methodInfo
    private String gateway;
    private String processingGateWay;

    // TrackingInfo
    private String source;
    private String sourceName;
    private String reference;
    private String sourceUrl;

    // CustomerInfo
    private String email;
    private String phone;
    private Integer customerId;

    // Status
    private String status;
    private String fulfillmentStatus;
    private String financialStatus;
    private String cancelReason;

    private AddressEsData billingAddress;
    private AddressEsData shippingAddress;
    private List<String> tags;

    private List<LineItemEsData> lineItems;

    private Integer locationId;

    private String currency;
    private BigDecimal unpaidAmount;

    @Getter
    @Setter
    public static class LineItemEsData {
        private int id;
        private Integer productId;
        private Integer variantId;

        private String title;
        private String variantTitle;
        private String name;
        private String sku;
        private String vendor;

        private int quantity;
        private BigDecimal price;
        private String fulfillmentStatus;
    }

    @Getter
    @Setter
    public static class AddressEsData extends AddressDto {
        private String name;
        private String nameNoSign;
        private String provinceDistrict;
        private String provinceDistrictNoSign;
        private String fullAddress;
    }
}
