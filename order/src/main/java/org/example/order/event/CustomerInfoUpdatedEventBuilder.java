package org.example.order.event;

import org.example.order.order.domain.order.model.OrderId;

import java.util.List;

public class CustomerInfoUpdatedEventBuilder extends AbstractEventBuilder<OrderId> {
    public CustomerInfoUpdatedEventBuilder(String verb, int subjectId, OrderId subject, int storeId, AuthorInfo author) {
        super(verb, subjectId, subject, storeId, author);
    }

    @Override
    public String buildEventDescription(String verb, OrderId subject) {
        return null;
    }

    @Override
    public String buildEventMessage(String verb, OrderId subject) {
        return null;
    }

    @Override
    public String buildEventPath(OrderId subject) {
        return null;
    }

    @Override
    public String getSubjectType() {
        return null;
    }

    @Override
    public List<Object> buildEventArgument(OrderId subject, AuthorInfo author) {
        return null;
    }
}
