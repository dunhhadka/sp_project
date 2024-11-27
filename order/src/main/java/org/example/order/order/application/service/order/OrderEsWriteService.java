package org.example.order.order.application.service.order;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.util.BinaryData;
import co.elastic.clients.util.ContentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.ddd.AppEventType;
import org.example.order.order.application.service.customer.Customer;
import org.example.order.order.application.service.customer.CustomerService;
import org.example.order.order.application.utils.ESSerializerUtils;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.domain.order.model.OrderLog;
import org.example.order.order.domain.order.model.es.OrderEsData;
import org.example.order.order.domain.order.model.es.OrderEsModel;
import org.example.order.order.infrastructure.data.dao.FulfillmentDao;
import org.example.order.order.infrastructure.data.dto.FulfillmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderEsWriteService {

    @Value("${spring.elasticsearch.order-type}")
    private String type;

    @Value("${spring.elasticsearch.order-index}")
    private String index;

    private final ElasticsearchClient elasticsearchClient;

    private final FulfillmentDao fulfillmentDao;

    private final CustomerService customerService;

    private final OrderMapper orderMapper;

    public void indexOrders(List<OrderLog> orderLogs) throws ExecutionException, InterruptedException, IOException {
        if (CollectionUtils.isEmpty(orderLogs)) return;

        var finalOrderLogs = getLastOrderLogsByOrderId(orderLogs);
        var partitionedLog = finalOrderLogs.stream()
                .collect(Collectors.partitioningBy(o -> o.getVerb() == AppEventType.delete));

        var deletedOrderLogs = partitionedLog.get(true);
        var addOrUpdateOrderLogs = partitionedLog.get(false);

        var addOrUpdateEsOrders = mapToEsModels(addOrUpdateOrderLogs);

        var bulkRequestBuilder = new BulkRequest.Builder();
        if (CollectionUtils.isNotEmpty(addOrUpdateEsOrders)) {
            for (OrderEsModel esOrder : addOrUpdateEsOrders) {
                processEvent(AppEventType.add, esOrder, bulkRequestBuilder);
            }
        }
        if (CollectionUtils.isNotEmpty(deletedOrderLogs)) {
            for (OrderLog orderLog : deletedOrderLogs) {
                var esOrder = new OrderEsModel(orderLog.getOrderId(), orderLog.getStoreId());
                processEvent(AppEventType.delete, esOrder, bulkRequestBuilder);
            }
        }
        var bulkRequest = bulkRequestBuilder.build();
        if (!bulkRequest.operations().isEmpty()) {
            var response = elasticsearchClient.bulk(bulkRequest);
            if (response.errors()) {
                response.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item -> log.error(item.error().reason()));
            }
        }
    }

    private void processEvent(AppEventType verb, OrderEsModel esOrder, BulkRequest.Builder bulkRequestBuilder) {
        if (verb == null || esOrder == null) return;
        var uid = Integer.toString(esOrder.getId());
        var routingKey = Integer.toString(esOrder.getStoreId());
        if (verb == AppEventType.delete) {
            var deleteRequest = new DeleteOperation.Builder()
                    .index(index)
                    .id(uid)
                    .routing(routingKey)
                    .build();
            var bulkOperation = new BulkOperation.Builder()
                    .delete(deleteRequest)
                    .build();
            bulkRequestBuilder.operations(bulkOperation);
        } else {
            var byteArrayData = BinaryData.of(ESSerializerUtils.marshalAsByte(esOrder), ContentType.APPLICATION_JSON);
            bulkRequestBuilder.operations(op -> op
                    .index(inx -> inx
                            .index(index)
                            .id(uid)
                            .routing(routingKey)
                            .document(byteArrayData)));
        }
    }

    private List<OrderEsModel> mapToEsModels(List<OrderLog> orderLogs) throws ExecutionException, InterruptedException {
        var orders = orderLogs.stream().map(orderLog -> JsonUtils.unmarshalEsData(orderLog.getData(), OrderEsData.class)).toList();
        var storeIds = orders.stream().map(OrderEsData::getStoreId).toList();
        var orderIds = orders.stream().map(OrderEsData::getId).toList();
        var customerIdMap = orders.stream().filter(o -> o.getCustomerId() != null)
                .collect(Collectors.groupingBy(OrderEsData::getStoreId,
                        Collectors.mapping(OrderEsData::getCustomerId, Collectors.toSet())));

        var asyncData = List.of(
                fulfillmentDao.getByStoreIdsAndOrderIdsAsync(storeIds, orderIds)
        );
        var data = CompletableFuture.allOf(asyncData.toArray(new CompletableFuture[0]))
                .thenApply(v -> asyncData.stream().map(CompletableFuture::join).toList())
                .get();

        var fulfillments = (List<FulfillmentDto>) data.get(0);

        List<Customer> customerList = new ArrayList<>();
        if (!customerIdMap.isEmpty()) {
            customerIdMap.forEach((storeId, ids) -> {
                var customers = customerService.findByIds(storeIds, ids);
                customerList.addAll(customers);
            });
        }

        return orders.stream().map(orderEsData -> {
                    var orderFulfillments = fulfillments.stream().filter(ff -> ff.getOrderId() == orderEsData.getId()).toList();
                    var customer = Optional.ofNullable(orderEsData.getCustomerId()).flatMap(id -> customerList.stream().filter(c -> c.getId() == id).findFirst())
                            .orElse(null);
                    return orderMapper.toEsModel(orderEsData, orderFulfillments, customer);
                })
                .toList();
    }

    private List<OrderLog> getLastOrderLogsByOrderId(List<OrderLog> orderLogs) {
        var orderLogMap = orderLogs.stream()
                .collect(groupingBy(OrderLog::getOrderId, maxBy(comparingInt(OrderLog::getOrderId))));
        return orderLogMap.values().stream().filter(Optional::isPresent).map(Optional::get).toList();
    }
}
