package org.example.order.order.application.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AddressHelper {

    private static final Pattern SPACES = Pattern.compile(" {2,}");

    public static String getFullName(String firstName, String lastName) {
        return Stream.of(firstName, lastName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));
    }

    public static Pair<String, String> getStructureName(String fullName) {
        String firstName = null;
        String lasName = null;
        if (StringUtils.isNotBlank(fullName)) {
            fullName = SPACES.matcher(fullName).replaceAll(" ").trim();
            int lastWhiteSpace = fullName.lastIndexOf(" ");
            if (lastWhiteSpace != -1) {
                firstName = fullName.substring(lastWhiteSpace + 1);
                lasName = fullName.substring(0, lastWhiteSpace);
            } else {
                firstName = fullName;
            }
        }
        return Pair.of(firstName, lasName);
    }

    public static AddressAreaModel resolve(AddressRequest request) {
        if (request == null) return new AddressAreaModel();

        Country country = null;
        Province province = null;
        District district = null;
        Ward ward = null;

        // country
        var allCountries = AddressUtils.countryList();
        if (request.getCountryId() != null) {
            country = allCountries.stream().filter(c -> c.getId() == request.getCountryId()).findFirst().orElse(null);
        }
        if (country == null && StringUtils.isNotBlank(request.getCountryCode())) {
            country = allCountries.stream().filter(c -> Objects.equals(c.getCode(), request.getCountryCode())).findFirst().orElse(null);
        }
        if (country == null && StringUtils.isNotBlank(request.getCountryName())) {
            country = allCountries.stream().filter(c -> Objects.equals(c.getName(), request.getCountryName())).findFirst().orElse(null);
        }

        var provinces = AddressUtils.provinceList();
        if (country != null) {
            int countryId = country.getId();
            provinces = provinces.stream().filter(p -> p.getCountryId() == countryId).toList();
        }
        if (request.getProvinceId() != null) {
            province = provinces.stream().filter(p -> p.getId() == request.getProvinceId()).findFirst().orElse(null);
        }
        if (StringUtils.isNotBlank(request.getProvinceCode())) {
            province = province = provinces.stream().filter(p -> Objects.equals(p.getCode(), request.getProvinceCode())).findFirst().orElse(null);
        }
        if (StringUtils.isNotBlank(request.getProvince())) {
            province = province = provinces.stream().filter(p -> Objects.equals(p.getName(), request.getProvince())).findFirst().orElse(null);
        }

        return new AddressAreaModel(country, province, district, ward);
    }

    @Getter
    @Setter
    public static class AddressRequest {
        private Integer countryId;
        private String countryCode;
        private String country;
        private String countryName;

        private Integer provinceId;
        private String provinceCode;
        private String province;
        private String city;

        private Integer districtId;
        private String districtCode;
        private String district;

        private Integer wardId;
        private String wardCode;
        private String ward;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressAreaModel {
        private Country country;
        private Province province;
        private District district;
        private Ward ward;
    }
}
