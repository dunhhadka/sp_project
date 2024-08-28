package org.example.order.order.application.converter;

import jakarta.persistence.AttributeConverter;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.utils.JsonUtils;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

public class StringListJoinAttributeConverter implements AttributeConverter<List<String>, String> {
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (CollectionUtils.isEmpty(attribute)) return null;
        return JsonUtils.joinString(attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (StringUtils.isNotBlank(dbData) && !dbData.equals(",")) {
            return Arrays.stream(StringUtils.split(dbData, ","))
                    .filter(StringUtils::isNotBlank)
                    .map(StringUtils::trim)
                    .distinct().toList();
        }
        return null;
    }
}
