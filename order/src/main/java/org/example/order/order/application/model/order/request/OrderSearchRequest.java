package org.example.order.order.application.model.order.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSearchRequest extends PagingFilterRequest {

    private String sort;
}
