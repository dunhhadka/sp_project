package org.example.order.order.domain.fulfillment.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import javax.validation.constraints.Size;

@Getter
@Embeddable
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OriginAddress {
    @Size
    private String name;
    @Size(max = 25)
    private String phone;
    @Size(max = 128)
    private String email;
    @Size(max = 255)
    private String address;
    @Size(max = 50)
    private String ward;
    @Size(max = 10)
    private String wardCode;
    @Size(max = 50)
    private String district;
    @Size(max = 10)
    private String districtCode;
    @Size(max = 50)
    private String province;
    @Size(max = 10)
    private String provinceCode;
    @Size(max = 50)
    private String city;
    @Size(max = 50)
    private String country;
    @Size(max = 10)
    private String countryCode;
    @Size(max = 20)
    private String zipCode;
}
