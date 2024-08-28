package org.example.order.order.application.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public final class JsonUtils {
    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
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
}
