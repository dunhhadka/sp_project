package org.example.order.order.application.service.order;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.model.order.request.OrderSearchRequest;
import org.example.order.order.application.model.order.response.OrderResponse;
import org.example.order.order.domain.order.model.es.OrderEsModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSearchService {

    @Value("${spring.elasticsearch.order-index}")
    private String index;

    @Value("${spring.elasticsearch.order-type}")
    private String type;

    private final ElasticsearchClient client;

    @SneakyThrows(IOException.class)
    public Pair<Long, List<OrderResponse>> search(Integer storeId, OrderSearchRequest request) {
        var searchRequest = new SearchRequest.Builder()
                .index(index)
                .routing(String.valueOf(storeId))
                .query(createSourceQueryForSearch(storeId, request))
                .build();
        var searchResult = client.search(searchRequest, OrderEsModel.class);
        return null;
    }

    private Query createSourceQueryForSearch(Integer storeId, OrderSearchRequest request) {
        return StoreSearchBuilder.search(storeId)
                .paginate(request)
                .query(rootBuilder -> mapToBoolQueryBuilder(rootBuilder, request))
                .sort(request.getSort(), true)
                .fetchSource(false)
                .toSearchQueryBuilder();
    }

    private void mapToBoolQueryBuilder(BoolQuery.Builder rootBuilder, OrderSearchRequest request) {

    }

}
