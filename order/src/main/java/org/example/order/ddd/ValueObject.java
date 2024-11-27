package org.example.order.ddd;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ValueObject<T> {
    public boolean sameAs(T other) {
        return this.equals(other);
    }

    @SneakyThrows
    protected List<Triple<String, Object, Object>> getDiff(T other) {
        if (this.sameAs(other)) return null;

        var result = new ArrayList<Triple<String, Object, Object>>();
        for (var field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            var value1 = field.get(this);
            var value2 = field.get(other);
            if (!Objects.equals(value1, value2)) {
                result.add(Triple.of(field.getName(), value1, value2));
            }
        }
        return result;
    }
}
