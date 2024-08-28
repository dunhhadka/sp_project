package org.example.order.order.domain.transaction.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AbstractEnumConverter<E extends Enum<E> & CustomValueEnum<String>> implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    public AbstractEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        if (attribute == null) return null;
        return attribute.getValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        E[] enums = enumType.getEnumConstants();
        for (E e : enums) {
            if (e.getValue().equals(dbData)) return e;
        }
        return null;
    }
}
