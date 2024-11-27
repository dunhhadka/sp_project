package org.example.order.order.application.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.domain.order.model.es.OrderEsModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public final class JsonUtils {
    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setDateFormat(new ISO8601DateFormat());

        // Register the JavaTimeModule to handle Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    public static <T> T unmarshal(InputStream inputStream, Class<T> clazz) throws IOException {
        return mapper.readValue(inputStream, clazz);
    }

    public static String joinString(List<String> stringList) {
        return joinString(stringList, ",");
    }

    private static String joinString(List<String> stringList, String delimiter) {
        if (stringList == null || stringList.isEmpty()) return StringUtils.EMPTY;
        if (delimiter == null) delimiter = ",";
        return stringList.stream().filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.joining(delimiter));
    }

    public static String marshal(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    public static <T> T unmarshal(String s, TypeReference<T> type) throws JsonProcessingException {
        return mapper.readValue(s, type);
    }

    public static <T> T unmarshal(String s, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(s, clazz);
    }

    @SneakyThrows
    public static String marshalLog(Object obj) {
        return marshal(obj);
    }

    public static <T> T unmarshalEsData(String data, Class<T> clazz) {
        try {
            return mapper.readValue(data, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't deserialize: " + e.getMessage() + data);
        }
    }

    public static byte[] marshalAsByte(OrderEsModel esOrder) {
        try {
            return mapper.writeValueAsBytes(esOrder);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't serialize: " + e.getMessage(), e);
        }
    }

    public static <T> String marshalAsJson(T object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
