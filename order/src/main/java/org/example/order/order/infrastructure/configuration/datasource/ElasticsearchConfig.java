package org.example.order.order.infrastructure.configuration.datasource;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient(@Value("${es.hosts}") List<String> hosts) {
        HttpHost[] httpHosts = hosts.stream()
                .map(address -> {
                    var hostAndPort = address.split(":");
                    return new HttpHost(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                })
                .toArray(HttpHost[]::new);
        RestClient restClient = RestClient.builder(httpHosts).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper()); // transport: chuyên trở
        return new ElasticsearchClient(transport);
    }
}
