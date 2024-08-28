package org.example.order.order.application.utils;

import java.io.IOException;
import java.util.List;

public final class AddressUtils {

    public static List<Country> countryList() {
        return CountriesHolder.countries;
    }

    public static List<Province> provinceList() {
        return ProvincesHolder.provinces;
    }

    private static class CountriesHolder {
        private static final List<Country> countries = List.of(readFile("json/country.json", Country[].class));
    }

    private static <T> T readFile(String path, Class<T> clazz) {
        var loader = AddressUtils.class.getClassLoader();
        try (var is = loader.getResourceAsStream(path)) {
            return JsonUtils.unmarshal(is, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ProvincesHolder {
        private static final List<Province> provinces = List.of(readFile("json/provinces.json", Province[].class));
    }
}
