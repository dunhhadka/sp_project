package org.example.order.order.application.service.order;

import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.response.*;
import org.example.order.order.application.utils.AddressHelper;
import org.example.order.order.infrastructure.data.dto.*;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring", imports = AddressHelper.class)
public abstract class OrderMapper {

    public abstract AddressHelper.AddressRequest toAddressRequest(OrderCreateRequest.AddressRequest address);

    @Mapping(target = "paymentGatewayNames", source = "gateWay", qualifiedByName = "paymentGatewayNames")
    public abstract OrderResponse fromDtoToResponse(OrderDto order);

    @Named("paymentGatewayNames")
    public List<String> paymentGatewayNames(String paymentGateWayNames) {
        if (StringUtils.isNotBlank(paymentGateWayNames)) {
            var seq = paymentGateWayNames.trim();
            return Arrays.stream(seq.split(",")).map(String::trim).distinct().toList();
        }
        return List.of();
    }

    public abstract LineItemResponse fromDtoToResponse(LineItemDto lineItem);

    public abstract TaxLineResponse fromDtoToResponse(TaxLineDto taxLineDto);

    public abstract DiscountAllocationResponse fromDtoToResponse(DiscountAllocationDto discountAllocationDto);

    public abstract ShippingLineResponse fromDtoToResponse(ShippingLineDto shippingLineDto);

    public abstract OrderDiscountResponse fromDtoToResponse(DiscountCodeDto discountCodeDto);

    public abstract DiscountApplicationResponse fromDtoToResponse(DiscountApplicationDto discountApplicationDto);

    @Mapping(target = "name", expression =
            "java(AddressHelper.getFullName(shippingAddressDto.getFirstName(), shippingAddressDto.getLastName()))")
    public abstract OrderAddressResponse fromDtoToResponse(ShippingAddressDto shippingAddressDto);

    @Mapping(target = "name", expression =
            "java(AddressHelper.getFullName(billingAddressDto.getFirstName(), billingAddressDto.getLastName()))")
    public abstract OrderAddressResponse fromDtoToResponse(BillingAddressDto billingAddressDto);

    @Mapping(target = "components", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "productExists", expression = "java(true)")
    public abstract CombinationLineResponse fromDtoToResponse(CombinationDto combinationDto);

    public abstract CombinationLineComponentResponse toCombinationLineComponentResponse(LineItemResponse lineItem);
}
