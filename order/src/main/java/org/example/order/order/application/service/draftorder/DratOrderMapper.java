package org.example.order.order.application.service.draftorder;

import org.example.order.order.application.model.draftorder.request.CombinationCalculateRequest;
import org.example.order.order.application.model.draftorder.request.DraftOrderAddressRequest;
import org.example.order.order.application.model.draftorder.request.DraftShippingLineRequest;
import org.example.order.order.application.utils.AddressHelper;
import org.example.order.order.domain.draftorder.model.DraftOrderLineItem;
import org.example.order.order.domain.draftorder.model.DraftOrderShippingLine;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class DratOrderMapper {

    public abstract List<CombinationCalculateRequest.LineItem> toCombinationCalculateItemRequest(List<DraftOrderLineItem> draftLineItems);
}
