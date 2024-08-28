package org.example.order.order.domain.transaction.model;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import org.example.order.order.application.utils.JsonUtils;

import java.util.Map;

@Converter
public class StringMapAttributeConverter implements AttributeConverter<Map<String, String>, String> {

    private static final TypeReference<Map<String, String>> MAP_STRING = new TypeReference<>() {
    };

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null) return null;
        return JsonUtils.marshal(attribute);
    }

    @SneakyThrows
    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return JsonUtils.unmarshal(dbData, MAP_STRING);
    }
}
