package org.example.order.order.domain.order.model.es;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class OrderEsModel {
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
    private Instant processedOn;
    private Instant closedOn;
    private Instant cancelledOn;

    // MoneyInfo
    private BigDecimal totalPrice;

    // PaymentMethodInfo
    private List<String> paymentGatewayNames;
    private String gateway;
    private String processingMethod;

    // TrackingInfo
    private String source;
    private String sourceName;

    private String email;
    private String phone;
    private Integer customerId;
    private CustomerEsModel customer;

    // Status
    private String status;
    private String fulfillmentStatus;
    private String financialStatus;
    private String cancelReason;

    private List<String> phones;
    private List<String> tags;

    private Integer locationId;

    private List<LineItemEsModel> lineItems;
    private List<ShippingLineEsModel> shippingLines;

    private List<FulfillmentEsModel> fulfillments;

    private AddressEsModel billingAddress;
    private AddressEsModel shippingAddress;

    private String currency;
    private BigDecimal unpaidAmount;

    private int printCount;

    private List<String> searchTexts;

    private List<String> orderPhones;

    public OrderEsModel(int orderId, int storeId) {
        this.id = orderId;
        this.storeId = storeId;
    }

    @Getter
    @Setter
    public static class CustomerEsModel {
        private int id;
        private String email;
        private String phone;
        private String name;
    }

    @Getter
    @Setter
    public static class LineItemEsModel {
        private Integer productId;
        private Integer variantId;
        @JsonIgnore
        private String name;
        private String sku;
        private String vendor;
    }

    @Getter
    @Setter
    public static class ShippingLineEsModel {
        private String title;
    }

    @Getter
    @Setter
    public static class FulfillmentEsModel {
        private String trackingNumber;
        private String trackingUrl;
        private String deliveryMethod;
        private String carrier;
        private String carrierName;
        private String shipmentStatus;
    }

    @Getter
    @Setter
    public static class AddressEsModel {
        private String countryCode;
        private String provinceCode;
        private String districtCode;
        private String wardCode;
        private String zip;
        private String company;
        private String provinceDistrict;
        private String provinceDistrictNoSign;
    }
}
