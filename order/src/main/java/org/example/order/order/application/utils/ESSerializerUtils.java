package org.example.order.order.application.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.math.BigDecimal;

public class ESSerializerUtils {
    private static final ObjectMapper jsonMapper;

    static {
        var stripTrailingZeroModule = new SimpleModule();
        var javaTimeModule = new JavaTimeModule();
        stripTrailingZeroModule.addSerializer(BigDecimal.class, new CustomDecimalSerializer());
        jsonMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .defaultDateFormat(new ISO8601DateFormat())
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .addModule(stripTrailingZeroModule)
                .addModule(javaTimeModule)
                .build();
    }

    public static <T> String marshalAsJson(T obj) {
        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't serialize");
        }
    }

    public static <T> byte[] marshalAsByte(T obj) {
        try {
            return jsonMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't serialize");
        }
    }

    private static class CustomDecimalSerializer extends JsonSerializer<BigDecimal> {

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializer) throws IOException {
            gen.writeNumber(value.stripTrailingZeros());
        }
    }
}
