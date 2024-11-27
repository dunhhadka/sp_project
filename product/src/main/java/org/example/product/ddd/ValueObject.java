package org.example.product.ddd;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ValueObject<T> {
    public boolean sameAs(T other) {
        return Objects.equals(this, other);
    }

    @SneakyThrows
    public List<Triple<String, Object, Object>> getDiffs(T other) {
        if (this.sameAs(other)) {
            return List.of();
        }

        var diffs = new ArrayList<Triple<String, Object, Object>>();
        for (var field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            var value1 = field.get(this);
            var value2 = field.get(other);
            if (!Objects.equals(value1, value2)) {
                diffs.add(Triple.of(field.getName(), value1, value2));
            }
        }
        return diffs;
    }
}
