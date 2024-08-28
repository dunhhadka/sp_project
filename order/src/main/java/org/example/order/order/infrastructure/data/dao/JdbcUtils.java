package org.example.order.order.infrastructure.data.dao;

import java.sql.Timestamp;
import java.time.Instant;

public final class JdbcUtils {
    public static Timestamp bindTimeStamp(Instant param) {
        if (param == null) return null;
        return Timestamp.from(param);
    }
}
