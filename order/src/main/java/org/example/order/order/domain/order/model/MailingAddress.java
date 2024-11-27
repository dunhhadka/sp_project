package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import lombok.*;
import org.example.order.order.application.utils.Country;
import org.example.order.order.application.utils.District;
import org.example.order.order.application.utils.Province;
import org.example.order.order.application.utils.Ward;

import javax.validation.constraints.Size;

@Getter
@Embeddable
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailingAddress {
    @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lastName;
    @Size(max = 50)
    private String phone;
    @Size(max = 255)
    private String address;
    @Size(max = 255)
    private String company;
    @Size(max = 50)
    private String city;
    @Size(max = 50)
    private String province; // tỉnh
    @Size(max = 50)
    private String district; // huyện
    @Size(max = 50)
    private String ward; // xã

    @Size(max = 10)
    private String countryCode;
    @Size(max = 10)
    private String provinceCode;
    @Size(max = 30)
    private String districtCode;
    @Size(max = 20)
    private String wardCode;

    @Size(max = 50)
    private String countryName;

    public MailingAddress(
            String firstName,
            String lastName,
            String phone,
            String address,
            String company,
            String city,
            Country country,
            Province province,
            District district,
            Ward ward
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.company = company;
        this.city = city;

        if (country != null) {
            this.countryCode = country.getCode();
            this.countryName = country.getName();

            if (province != null) {
                this.provinceCode = province.getCode();
                this.province = province.getName();

                if (district != null) {
                    this.districtCode = district.getCode();
                    this.district = district.getName();

                    if (ward != null) {
                        this.wardCode = ward.getCode();
                        this.ward = ward.getName();
                    }
                }
            }
        }
    }

}
