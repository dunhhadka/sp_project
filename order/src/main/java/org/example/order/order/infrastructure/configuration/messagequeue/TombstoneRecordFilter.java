package org.example.order.order.infrastructure.configuration.messagequeue;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

public class TombstoneRecordFilter<K, V> implements RecordFilterStrategy<K, V> {
    public static final String KAFKA_MESSAGE_IGNORE_PAYLOAD = "{\"schema\":null,\"payload\":null}";

    @Override
    public boolean filter(ConsumerRecord<K, V> consumerRecord) {
        return consumerRecord.value() == null || KAFKA_MESSAGE_IGNORE_PAYLOAD.equals(consumerRecord.value());
    }
}
