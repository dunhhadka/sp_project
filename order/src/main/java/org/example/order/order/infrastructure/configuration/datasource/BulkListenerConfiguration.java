package org.example.order.order.infrastructure.configuration.datasource;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BulkListenerConfiguration {


    @Bean
    public BulkListener<String> bulkListener() {
        return new BulkListener<>() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request, List<String> contexts) {
                log.info("starting index request");
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, List<String> contexts, BulkResponse response) {
                log.info("finish index request");
                log.debug("Bulk request " + executionId + " completed " + contexts);
                for (int i = 0; i < contexts.size(); i++) {
                    BulkResponseItem item = response.items().get(i);
                    if (item.error() != null) {
                        log.error("Failed to index file " + contexts.get(i) + " - " + item.error().reason());
                    }
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, List<String> contexts, Throwable failure) {
                log.info("finish with failure index request");
                log.debug("Bulk request " + executionId + " failed", failure);
            }
        };
    }

    @Bean
    public BulkIngester<String> bulkIngester(ElasticsearchClient elasticsearchClient, BulkListener<String> listener) {
        return BulkIngester.of(b -> b
                .client(elasticsearchClient)
                .listener(listener)
                .maxOperations(500)
                .flushInterval(1, TimeUnit.SECONDS));
    }
}
