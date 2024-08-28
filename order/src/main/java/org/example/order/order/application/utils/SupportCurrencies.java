package org.example.order.order.application.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.order.order.infrastructure.data.dto.CurrencyDto;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public final class SupportCurrencies {

    private static class Holder {
        private static final Map<String, CurrencyDto> _currencies;

        static {
            log.debug("resole currency");
            String fileName = "json/currencies.json";
            var loader = SupportCurrencies.class.getClassLoader();
            try (var inputStream = loader.getResourceAsStream(fileName)) {
                _currencies = Arrays
                        .stream(JsonUtils.unmarshal(inputStream, CurrencyDto[].class))
                        .collect(Collectors.toMap(CurrencyDto::getCode, Function.identity()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static CurrencyDto getByCode(String currencyCode) {
        return Holder._currencies.get(currencyCode);
    }
}
