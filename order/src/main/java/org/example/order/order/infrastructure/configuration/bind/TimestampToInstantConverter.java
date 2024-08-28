package org.example.order.order.infrastructure.configuration.bind;

import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.time.OffsetDateTime;

public class TimestampToInstantConverter implements Converter<OffsetDateTime, Instant> {
    @Override
    public Instant convert(OffsetDateTime source) {
        return source.toInstant();
    }
}
