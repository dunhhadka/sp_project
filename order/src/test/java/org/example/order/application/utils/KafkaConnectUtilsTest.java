package org.example.order.application.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.utils.KafkaConnectUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KafkaConnectUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = """
            {
              "schema": {
                "type": "struct",
                "fields": [
                  {"type": "int32","optional": false,"field": "Id"},
                  {"type": "int32","optional": false,"field": "StoreId"},
                  {"type": "string","optional": true,"field": "Data"},
                  {"type": "int64","optional": false,"name": "org.apache.kafka.connect.data.Timestamp","version": 1,"field": "CreatedOn"},
                  {"type": "string","optional": true,"field": "Status"}
                ],
                "optional": false,
                "name": "ProductLogs"
              },
              "payload": {
                "Id": 455035894,
                "StoreId": 438408,
                "Data": "abc",
                "CreatedOn": 1681898491760,
                "Status": "ACTIVE"
              }
            }
            """)
    void unmarshalPOJO(String unwrappedValue) throws JsonProcessingException {
        var productLog = KafkaConnectUtils.unmarshalPOJO(unwrappedValue, ProductLog.class);
        assertNotNull(productLog);
        assertEquals(productLog.getId(), 455035894);
        assertEquals(productLog.getStoreId(), 438408);
        assertEquals(productLog.getData(), "abc");
        assertEquals(productLog.getStatus(), ProductLog.Status.active);
    }

    @Getter
    @Setter
    static
    class ProductLog {
        private int id;
        private int storeId;
        private String data;
        private Instant createdOn;
        private Status status;

        public enum Status {
            active, inactive;

            @JsonCreator
            public static Status fromString(String key) {
                return key == null
                        ? null
                        : Status.valueOf(key.toLowerCase());
            }
        }
    }
}
