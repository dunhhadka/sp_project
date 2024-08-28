package org.example.order.order.application.model.order.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.order.model.CombinationLine;
import org.example.order.order.domain.order.model.DiscountAllocation;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.domain.order.model.OrderDiscountCode;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderCreateRequest {

    private UUID clientId;

    @JsonAlias("processed_at")
    private Instant processedOn;

    private @Size(max = 32) String cartToken;
    private @Size(max = 32) String checkoutToken;

    private @Size(max = 50) String source;
    private @Size(max = 50) String sourceName;

    private @Size(max = 128) String email;
    private @Size(max = 21) String phone;

    private boolean buyerAcceptMarketing;
    private @Valid CustomerRequest customer;

    private @Valid AddressRequest billingAddress;
    private @Valid AddressRequest shippingAddress;

    private @NotEmpty @Size(max = 100) List<@Valid LineItemRequest> lineItems;
    private @Size(max = 100) List<@Valid ShippingLineRequest> shippingLines;

    private @Min(0) BigDecimal totalDiscount;
    private @Size(max = 10) List<@Valid DiscountCodeRequest> discountCodes;

    private @Size(max = 100) List<@Valid DiscountApplicationRequest> discountApplications;

    private @Size(max = 250) String currency;

    private @Size(max = 250) String gateWay;

    private @Size(max = 20) String processingMethod;

    private @Min(0) int totalWeight;

    private @Size(max = 2000) String landingSite;
    private @Size(max = 2000) String landingSiteRef;
    private @Size(max = 2000) String referringSite;
    private @Size(max = 2000) String reference;

    private @Size(max = 255) String sourceIdentifier;

    private @Size(max = 255) String sourceUrl;

    private @Size(max = 2000) String note;

    private @Size(max = 50) List<@Size(max = 250) String> tags;

    private Integer locationId;

    private @Size(max = 100) List<@Valid CombinationLineRequest> combinationLines;

    private @Size(max = 50) List<@Valid TaxLineRequest> taxLines;

    private boolean taxExempt;

    private boolean taxesIncluded;

    @Size(max = 1, message = "Only one fulfillment is supported for now")
    private List<@Valid FulfillmentRequest> fufillments;

    private List<@Valid TransactionCreateRequest> transactions;

    @Getter
    @Setter
    public static class CustomerRequest {
        private @Min(0) int id;
        private @Size(max = 128) String email;
        private @Size(max = 21) String phone;

        public boolean isEmpty() {
            return id == 0 || StringUtils.isBlank(this.email) && StringUtils.isBlank(this.phone);
        }
    }

    @Getter
    @Setter
    public static class AddressRequest {
        private @Size(max = 50) String firstName;
        private @Size(max = 50) String lastName;
        private @Size(max = 100) String name;

        private @Size(max = 250) String phone;
        private @Size(max = 255) String address;
        private @Size(max = 255) String company;

        private @Min(0) Integer countryId;
        private @Size(max = 50) String countryCode;
        private @Size(max = 50) String country;
        private @Size(max = 50) String countryName;

        @Min(0)
        private Integer provinceId;
        @Size(max = 10)
        private String provinceCode;
        @Size(max = 50)
        private String province;
        @Size(max = 50)
        private String city;

        @Min(0)
        private Integer districtId;
        @Size(max = 30)
        private String districtCode;
        @Size(max = 50)
        private String district;

        @Min(0)
        private Integer wardId;
        @Size(max = 20)
        private String wardCode;
        @Size(max = 50)
        private String ward;
    }

    @Getter
    @Setter
    public static class LineItemRequest {
        private Integer variantId;

        private @Min(0) BigDecimal price;
        private @Min(0) BigDecimal totalDiscount;
        private @Positive int quantity;
        private @Min(0) int grams;
        private Boolean requireShipping;

        private @Size(max = 255) String discountCode;

        private @Size(max = 320) String title;
        private @Size(max = 500) String variantTitle;
        private @Size(max = 50) String sku;
        private @Size(max = 100) String vendor;
        private @Size(max = 50) String unit;

        private boolean giffCard;

        private Boolean taxable;

        private @Size(max = 100) List<@Valid DiscountAllocationRequest> discountAllocations;

        private @Size(max = 100) List<@Valid TaxLineRequest> taxLines;

        private Integer combinationLineIndex;
    }

    @Getter
    @Setter
    @Builder
    public static class DiscountAllocationRequest {
        private @NotNull BigDecimal amount;

        private BigDecimal originalPrice;
        private BigDecimal discountedPrice;

        private DiscountAllocation.TargetType targetType;
        private int targetId;

        @JsonAlias({"apply_index", "application_index"})
        private int discountApplicationIndex;
    }

    @Getter
    @Setter
    @Builder(toBuilder = true)
    public static class TaxLineRequest {
        @NotNull
        @Min(0)
        private BigDecimal rate; // tỷ lệ

        private String title = "tax";

        private BigDecimal price = BigDecimal.ZERO;

        public String getKey() {
            return this.title + this.rate;
        }

        public void addPrice(BigDecimal price) {
            if (this.price == null && price == null) {
                this.price = BigDecimal.ZERO;
            } else if (this.price == null) {
                this.price = price;
            } else if (price != null) {
                this.price = this.price.add(price);
            }
        }
    }

    @Getter
    @Setter
    public static class ShippingLineRequest {
        private @NotBlank @Size(max = 150) String title;
        private @Size(max = 150) String code;
        private @Size(max = 50) String source;

        private @NotNull @Min(0) BigDecimal price;

        private @Size(max = 100) List<@Valid DiscountAllocationRequest> discountAllocations;

        private @Size(max = 100) List<@Valid TaxLineRequest> taxLines;
    }

    @Getter
    @Setter
    public static class DiscountCodeRequest {
        private @Size(max = 255) String code;
        private @Min(0) BigDecimal amount;

        private OrderDiscountCode.ValueType type = OrderDiscountCode.ValueType.fixed_amount;

        private Boolean custom = true;

        private String title;

        private String description;
    }

    @Getter
    @Setter
    @Builder
    public static class DiscountApplicationRequest {
        @Size(max = 255)
        private String code;

        @Size(max = 250)
        private String title;

        @Size(max = 250)
        private String description;

        @NotNull
        private BigDecimal value;

        private @NotNull DiscountApplication.ValueType valueType;

        private @NotNull DiscountApplication.TargetType targetType;

        private DiscountApplication.RuleType ruleType = DiscountApplication.RuleType.product;

        private int index;
    }

    @Getter
    @Setter
    public static class CombinationLineRequest {
        private @Min(1) long variantId;
        private @Min(1) long productId;
        private @Min(0) BigDecimal price;
        private @DecimalMin(value = "0.001") BigDecimal quantity;
        private @Size(max = 320) String title;
        private @Size(max = 500) String variantTitle;
        private @Size(max = 255) String vendor;
        private @Size(max = 50) String sku;
        private @Size(max = 50) String unit;
        private @Size(max = 50) String itemUnit;
        private @NotNull CombinationLine.Type type;
    }

    @Getter
    @Setter
    public static class FulfillmentRequest {
        @NotNull
        private Fulfillment.DeliveryMethod deliveryMethod;
        private Fulfillment.ShipmentStatus deliveryStatus;
        private boolean sendNotification;
        @Valid
        private TrackingInfoInput trackingInfo;
        @Valid
        private PickupAddressInput pickupAddress;
        @Valid
        private ShippingAddressInput shippingAddress;
        @Size(max = 500)
        private String note;
    }

    @Getter
    @Setter
    public static class TrackingInfoInput {
        private String trackingCompany;
        private String carrier;
        private String carrierName;
        private List<@Size(max = 50) String> trackingNumbers;
        private List<@Size(max = 255) String> trackingUrls;
    }

    @Getter
    @Setter
    public static class PickupAddressInput {
        @NotNull
        private Long locationId;
        @Size(max = 128)
        private String name;
        @Size(max = 128)
        private String email;
        @Size(max = 25)
        private String phone;
        @NotBlank
        @Size(max = 255)
        private String address;
        @Size(max = 20)
        private String zipCode;
    }

    @Getter
    @Setter
    public static class ShippingAddressInput {
        @Size(max = 50)
        private String firstName;

        @Size(max = 50)
        private String lastName;

        @Size(max = 100)
        private String fullName;

        @Size(max = 128)
        private String email;

        @Size(max = 25)
        private String phone;

        @NotBlank
        @Size(max = 255)
        private String address;

        @Size(max = 50)
        private String latitude;

        @Size(max = 50)
        private String longitude;

        @Size(max = 20)
        private String zipCode;
    }

}
