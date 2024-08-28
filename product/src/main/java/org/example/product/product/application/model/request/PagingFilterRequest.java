package org.example.product.product.application.model.request;

public class PagingFilterRequest {

    private int page = 1;
    private int limit = 50;

    public int getPage() {
        if (page <= 0) return 1;
        return page;
    }

    public int getLimit() {
        if (limit <= 0) return 50;
        else return Math.min(250, limit);
    }
}
