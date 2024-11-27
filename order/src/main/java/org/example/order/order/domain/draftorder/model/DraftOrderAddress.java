package org.example.order.order.domain.draftorder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DraftOrderAddress {
    @Size(max = 50)
    private String firstName;
    @Size(max = 255)
    private String address1;
    @Size(max = 250)
    private String phone;
    @Size(max = 50)
    private String city;
    @Size(max = 20)
    private String zip;
    @Size(max = 50)
    private String province;
    @Size(max = 50)
    private String country;
    @Size(max = 50)
    private String lastName;
    @Size(max = 255)
    private String address2;
    @Size(max = 255)
    private String company;
    @Size(max = 10)
    private String countryCode;
    @Size(max = 10)
    private String provinceCode;
    @Size(max = 50)
    private String district;
    @Size(max = 30)
    private String districtCode;
    @Size(max = 50)
    private String ward;
    @Size(max = 20)
    private String wardCode;
    @Size(max = 50)
    private String countryName;
    @Size(max = 50)
    private String latitude;
    @Size(max = 50)
    private String longitude;
    @Size(max = 101)
    private String name;
}
