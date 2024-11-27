package org.example.order.order.application.service.order;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.response.*;
import org.example.order.order.application.service.customer.Customer;
import org.example.order.order.application.utils.AddressHelper;
import org.example.order.order.application.utils.CustomerPhoneUtils;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.order.model.es.OrderEsData;
import org.example.order.order.domain.order.model.es.OrderEsModel;
import org.example.order.order.infrastructure.data.dto.*;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


    @Mapping(target = "fulfillments", source = "fulfillments", qualifiedByName = "toSuccessFulfillmentEsModelList")
    @Mapping(target = "paymentGatewayNames", source = "orderEsData.gateway", qualifiedByName = "paymentGatewayNames")
    @Mapping(target = "id", source = "orderEsData.id")
    @Mapping(target = "phone", source = "orderEsData.phone")
    @Mapping(target = "email", source = "orderEsData.email")
    @Mapping(target = "tags", source = "orderEsData.tags")
    @Mapping(target = "createdOn", source = "orderEsData.createdOn")
    @Mapping(target = "modifiedOn", source = "orderEsData.modifiedOn")
    public abstract OrderEsModel toEsModel(OrderEsData orderEsData, List<FulfillmentDto> fulfillments, Customer customer);

    @AfterMapping
    public void afterToEsModel(@MappingTarget OrderEsModel orderEsModel, OrderEsData orderEsData, List<FulfillmentDto> fulfillments) {
        List<String> otherTexts = new ArrayList<>();
        fillPhones(orderEsModel, orderEsData);

        var billingAddress = orderEsData.getBillingAddress();
        var shippingAddress = orderEsData.getShippingAddress();

        otherTexts.addAll(getSearchTextsFromAddress(shippingAddress));
        otherTexts.addAll(getSearchTextsFromAddress(billingAddress));

        var variantIds = orderEsModel.getLineItems().stream()
                .map(OrderEsModel.LineItemEsModel::getVariantId)
                .filter(NumberUtils::isPositive)
                .map(String::valueOf)
                .distinct().toList();
        var productNames = orderEsModel.getLineItems().stream()
                .map(OrderEsModel.LineItemEsModel::getName)
                .filter(StringUtils::isNotBlank)
                .distinct().toList();
        var skus = orderEsModel.getLineItems().stream()
                .map(OrderEsModel.LineItemEsModel::getSku)
                .filter(StringUtils::isNotBlank)
                .distinct().toList();
        var vendors = orderEsModel.getLineItems().stream()
                .map(OrderEsModel.LineItemEsModel::getVendor)
                .filter(StringUtils::isNotBlank)
                .distinct().toList();
        otherTexts.addAll(variantIds);
        otherTexts.addAll(productNames);
        otherTexts.addAll(skus);
        otherTexts.addAll(vendors);

        fillSearchText(orderEsModel, otherTexts);
    }

    private void fillSearchText(OrderEsModel orderEsModel, List<String> others) {
        var orderPhones = this.handleEsPhone(orderEsModel.getPhone());
        orderEsModel.setOrderPhones(orderPhones);

        if (CollectionUtils.isNotEmpty(others)) {
            var searchTexts = others.stream().filter(StringUtils::isNotBlank).distinct().toList();
            orderEsModel.setSearchTexts(searchTexts);
        }
    }

    private <T extends AddressDto> List<String> getSearchTextsFromAddress(T address) {
        if (address == null) return List.of();
        return Stream.of(
                getFullName(address),
                getFullAddress(address),
                address.getCompany()
        ).filter(StringUtils::isNotBlank).distinct().toList();
    }

    private <T extends AddressDto> String getFullAddress(T address) {
        if (address == null) return null;
        return Stream.of(
                address.getAddress(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince()
        ).filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining(" "));
    }

    private <T extends AddressDto> String getFullName(T address) {
        if (address == null) return null;
        if (StringUtils.isAllBlank(address.getFirstName(), address.getLastName())) return null;
        return AddressHelper.getFullName(address.getFirstName(), address.getLastName());
    }

    private void fillPhones(OrderEsModel model, OrderEsData data) {
        var billing = data.getBillingAddress();
        var shipping = data.getShippingAddress();

        List<String> result = new ArrayList<>();
        if (billing != null) {
            result.addAll(handleEsPhone(billing.getPhone()));
        }
        if (shipping != null) {
            result.addAll(handleEsPhone(shipping.getPhone()));
        }
        var phones = result.stream().distinct().toList();
        model.setPhones(phones);
    }

    private List<String> handleEsPhone(String phone) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isNotBlank(phone)) {
            var resPhone = CustomerPhoneUtils.normalize(phone);
            if (resPhone != null) {
                result.add(resPhone);
                if (resPhone.startsWith("+84")) result.add(String.format("0%s", resPhone.substring(3)));
            }
        }
        return result;
    }

    @Named("toSuccessFulfillmentEsModelList")
    protected List<OrderEsModel.FulfillmentEsModel> toSuccessFulfillmentEsModelList(List<FulfillmentDto> fulfillments) {
        if (CollectionUtils.isEmpty(fulfillments)) return List.of();
        return fulfillments.stream()
                .filter(ff -> ff.getStatus() == Fulfillment.FulfillStatus.success)
                .map(this::toFulfillmentEsModel)
                .toList();
    }

    public abstract OrderEsModel.FulfillmentEsModel toFulfillmentEsModel(FulfillmentDto fulfillment);
}
