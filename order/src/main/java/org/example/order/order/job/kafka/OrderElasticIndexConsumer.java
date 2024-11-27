package org.example.order.order.job.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.service.order.OrderEsWriteService;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.domain.order.model.OrderLog;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderElasticIndexConsumer {

    private final OrderEsWriteService orderEsWriteService;

//    @KafkaListener(
//            topics = "${spring.kafka.order.topic.order-log}",
//            groupId = "${spring.kafka.order.group.es-order-indexer}",
//            containerFactory = "batchConcurrentKafkaListenerContainerFactory"
//    )
//    public void listen(List<String> messages) throws IOException, ExecutionException, InterruptedException {
//        List<OrderLog> orderLogs = new ArrayList<>(messages.size());
//        for (var message : messages) {
//            orderLogs.add(JsonUtils.unmarshal(message, OrderLog.class));
//        }
//        orderEsWriteService.indexOrders(orderLogs);
//    }
}
