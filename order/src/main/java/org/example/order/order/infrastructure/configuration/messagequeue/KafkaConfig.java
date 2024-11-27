package org.example.order.order.infrastructure.configuration.messagequeue;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${kafka.broker.address}")
    private String kafkaServerUri;

    @Value("${spring.kafka.listener.auto-startup}")
    private boolean autoStartup;

    @Bean("kafkaTemplate")
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean("kafkaProducerFactory")
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServerUri);
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 2097152); // 2MB
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 2097152);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(getConsumerConfigs());
    }

    private Map<String, Object> getConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServerUri);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 10 * 1024 * 1024);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60 * 1000);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 70 * 1000);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "default");
        return props;
    }

    private static final int DEFAULT_MAX_FAILURES = 10;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> batchConcurrentKafkaListenerContainerFactory(
            KafkaProperties properties,
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer
    ) {
        var consumerProperties = properties.buildConsumerProperties(null);
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        var listenerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        var consumerFactory = new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(null));
        configurer.configure(listenerFactory, consumerFactory);

        listenerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        listenerFactory.setBatchListener(true);

        listenerFactory.setCommonErrorHandler(new DefaultErrorHandler((consumerRecord, exception) -> {
            throw new RuntimeException("Error after 10 times retry handle message");
        }, new FixedBackOff(0, DEFAULT_MAX_FAILURES - 1)));

        return listenerFactory;
    }
}
