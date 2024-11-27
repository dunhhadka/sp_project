package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.fulfillment.model.Fulfillment;

import java.time.Instant;

@Getter
@Setter
public class FulfillmentDto {
    private Integer id;
    private String name;
    private String number;
    private Integer storeId;
    private Integer orderId;
    private Fulfillment.FulfillStatus status;
    private String trackingCompany;
    private String trackingNumber;
    private String trackingUrl;
    private Instant createdOn;
    private Instant modifiedOn;
    private String receipt;
    private String trackingNumbers;
    private String trackingUrls;

    // new fields v3
    private Long locationId;
    private String deliveryMethod;
    private String carrier;
    private String carrierName;
    private String shipmentStatus;
    private String originAddressName;
    private String originAddressPhone;
    private String originAddressEmail;
    private String originAddressAddress1;
    private String originAddressAddress2;
    private String originAddressWard;
    private String originAddressWardCode;
    private String originAddressDistrict;
    private String originAddressDistrictCode;
    private String originAddressProvince;
    private String originAddressProvinceCode;
    private String originAddressCity;
    private String originAddressCountry;
    private String originAddressCountryCode;
    private String originAddressZipCode;
    private Boolean notifyCustomer;
    private Instant deliveredOn;
}
