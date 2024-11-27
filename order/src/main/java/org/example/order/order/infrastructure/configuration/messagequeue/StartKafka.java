package org.example.order.order.infrastructure.configuration.messagequeue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "spring.kafka.listener.auto-startup", matchIfMissing = false)
public class StartKafka implements CommandLineRunner {
    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    public StartKafka(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("spring kafka is starting...");
        kafkaListenerEndpointRegistry.start();
    }
}
