package org.example.order.order.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.ddd.AppEventType;
import org.example.order.order.application.service.order.OrderEsWriteService;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.model.OrderIdGenerator;
import org.example.order.order.domain.order.model.OrderLog;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {

    @Value("${spring.kafka.order.topic.order-log}")
    private String orderTopic;

    private final OrderIdGenerator orderIdGenerator;

    // để tạm
    private final OrderEsWriteService orderEsWriteService;

    @PersistenceContext
    private final EntityManager entityManager;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void save(Order order) {

        var isNew = order.isNew();
        var eventType = isNew ? AppEventType.add : AppEventType.update;
        var orderLog = generateOrderLog(order, eventType);

        if (order.isNew()) entityManager.persist(order);
        else entityManager.merge(order);

        entityManager.persist(orderLog);

        entityManager.flush();

//        try {
//            kafkaTemplate.send(orderTopic, JsonUtils.marshal(orderLog));
//        } catch (JsonProcessingException e) {
//            log.error("send to kafka error");
//        }

        try {
            orderEsWriteService.indexOrders(List.of(orderLog));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OrderLog generateOrderLog(Order order, AppEventType verb) {
        return new OrderLog(
                order.getId().getStoreId(),
                order.getId().getId(),
                verb,
                verb != AppEventType.delete
                        ? JsonUtils.marshalLog(order)
                        : null
        );
    }

    @Override
    public Order findById(OrderId id) {
        var order = entityManager.find(Order.class, id);
        if (order != null) {
            order.setIdGenerator(orderIdGenerator);
        }
        return order;
    }
}
