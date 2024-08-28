package org.example.order.order.application.model.order.request;

import org.example.order.order.application.utils.NumberUtils;

public class PagingFilterRequest {

    private int limit;
    private int page;

    public void setLimit(Integer limit) {
        if (!NumberUtils.isPositive(limit))
            this.limit = 50;
        else this.limit = Math.min(limit, 250);
    }

    public void setPage(Integer page) {
        if (!NumberUtils.isPositive(page))
            this.page = 1;
        else this.page = page;
    }

    public int getLimit() {
        if (limit <= 0) return 50;
        return Math.min(limit, 250);
    }

    public int getPage() {
        if (page <= 0) return 1;
        return page;
    }
}
