package org.example.order.order.application.service.order;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.example.order.order.application.model.order.request.PagingFilterRequest;

import java.util.function.Consumer;

public class StoreSearchBuilder {

    private final BoolQuery.Builder rootQueryBuilder;
    private int from = 0;
    private int size = 50;

    private boolean source = true;

    public static StoreSearchBuilder search(int storeId) {
        return new StoreSearchBuilder().storeId(storeId);
    }

    private StoreSearchBuilder paginate(int from, int size) {
        from = from;
        size = size;
        return this;
    }

    private StoreSearchBuilder storeId(int storeId) {
        this.rootQueryBuilder.filter(QueryBuilders.term(
                builder -> builder.queryName("store_id").value(storeId)));
        return this;
    }

    private StoreSearchBuilder() {
        rootQueryBuilder = new BoolQuery.Builder();
    }

    public StoreSearchBuilder paginate(PagingFilterRequest request) {
        this.paginate((request.getPage() - 1) * request.getLimit(), request.getLimit());
        return this;
    }


    public StoreSearchBuilder query(Consumer<BoolQuery.Builder> consumer) {
        consumer.accept(rootQueryBuilder);
        return this;
    }

    public StoreSearchBuilder sort(String sort, boolean useDefault) {
        return this;
    }

    public StoreSearchBuilder fetchSource(boolean fetchSource) {
        this.source = fetchSource;
        return this;
    }

    public Query toSearchQueryBuilder() {
        return Query.of(builder -> builder.bool(rootQueryBuilder.build()));
    }
}
