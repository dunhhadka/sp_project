package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressDto {
    private int id;
    private int storeId;
    private int orderId;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String company;
    private String city;
    private String province; // tỉnh
    private String district; // huyện
    private String ward; // xã

    private String countryCode;
    private String provinceCode;
    private String districtCode;
    private String wardCode;

    private String countryName;
}
