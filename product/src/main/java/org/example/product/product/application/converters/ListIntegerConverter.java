package org.example.product.product.application.converters;

import jakarta.persistence.AttributeConverter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ListIntegerConverter implements AttributeConverter<List<Integer>, String> {

    @Override
    public String convertToDatabaseColumn(List<Integer> inputs) {
        if (CollectionUtils.isEmpty(inputs)) return null;
        return inputs.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (StringUtils.isEmpty(dbData)) return List.of();
        return Arrays.stream(dbData.split(","))
                .filter(this::isNumber)
                .map(Integer::valueOf)
                .toList();
    }

    private boolean isNumber(String s) {
        Integer number = null;
        try {
            number = Integer.valueOf(s);
        } catch (Exception ignored) {
        }
        return number != null;
    }
}
