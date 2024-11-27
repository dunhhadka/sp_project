package org.example.order.order.application.model.draftorder.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
public class DraftOrderAddressRequest {
    @Size(max = 255)
    private String address1;

    @Size(max = 255)
    private String address2;

    @Size(max = 50)
    private String city;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 250)
    private String phone;

    @Size(max = 50)
    private String province;
    @Size(max = 10)
    private String provinceCode;

    @Size(max = 20)
    private String zip;

    @Size(max = 10)
    private String countryCode;
    @Size(max = 50)
    private String country;
    @Size(max = 50)
    private String countryName;

    @Size(max = 101)
    private String name;

    @Size(max = 255)
    private String company;

    @Size(max = 50)
    private String district;
    @Size(max = 30)
    private String districtCode;

    @Size(max = 50)
    private String ward;
    @Size(max = 20)
    private String wardCode;

    @Size(max = 50)
    private String latitude;
    @Size(max = 50)
    private String longitude;

    private int countryId;
    private int provinceId;
    private int districtId;
    private int wardId;
}
