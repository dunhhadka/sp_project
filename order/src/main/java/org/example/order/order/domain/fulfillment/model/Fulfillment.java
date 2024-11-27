package org.example.order.order.domain.fulfillment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.utils.NumberUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Entity
@Table(name = "fulfillments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Fulfillment extends AggregateRoot<Fulfillment> {

    @Transient
    @JsonIgnore
    private FulfillmentIdGenerator idGenerator;

    @EmbeddedId
    @JsonUnwrapped
    @AttributeOverride(name = "storeId", column = @Column(name = "storeId"))
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    private FulfillmentId id;

    @Size(max = 255)
    private String name;

    private Integer number;

    private int orderId;

    private Long locationId;

    @Column(columnDefinition = "DATETIME2")
    private Instant createdOn;

    @Column(columnDefinition = "DATETIME2")
    private Instant cancelledOn;

    @Column(columnDefinition = "DATETIME2")
    private Instant deliveredOn;

    private boolean notifyCustomer;

    @Enumerated(value = EnumType.STRING)
    private FulfillStatus fulfillStatus;

    @Enumerated(value = EnumType.STRING)
    public ShipmentStatus shipmentStatus;

    @OneToMany(mappedBy = "aggRoot", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<FulfillmentLineItem> lineItems;

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "originAddressName")),
            @AttributeOverride(name = "email", column = @Column(name = "originAddressEmail")),
            @AttributeOverride(name = "phone", column = @Column(name = "originAddressPhone")),
            @AttributeOverride(name = "address", column = @Column(name = "originAddressAddress")),
            @AttributeOverride(name = "ward", column = @Column(name = "originAddressWard")),
            @AttributeOverride(name = "wardCode", column = @Column(name = "originAddressWardCode")),
            @AttributeOverride(name = "district", column = @Column(name = "originAddressDistrict")),
            @AttributeOverride(name = "districtCode", column = @Column(name = "originAddressDistrictCode")),
            @AttributeOverride(name = "province", column = @Column(name = "originAddressProvince")),
            @AttributeOverride(name = "provinceCode", column = @Column(name = "originAddressProvinceCode")),
            @AttributeOverride(name = "city", column = @Column(name = "originAddressCity")),
            @AttributeOverride(name = "country", column = @Column(name = "originAddressCountry")),
            @AttributeOverride(name = "countryCode", column = @Column(name = "originAddressCountryCode")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "originAddressZipCode"))})
    private OriginAddress originAddress;

    @Embedded
    @Valid
    private TrackingInfo trackingInfo;

    @Enumerated(value = EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    public void updateEffectiveQuantity(Map<Long, Integer> modifiedMap, EffectQuantityType effectQuantityType) {
        if (CollectionUtils.isEmpty(modifiedMap) || CollectionUtils.isEmpty(this.lineItems)) return;
        for (var lineItem : lineItems) {
            var modifiedQuantity = modifiedMap.get((long) lineItem.getLineItemId());
            if (!NumberUtils.isPositive(modifiedQuantity)) {
                continue;
            }
            var effectiveLineQuantity = lineItem.getEffectiveQuantity();
            var newQuantity = effectQuantityType == EffectQuantityType.add
                    ? effectiveLineQuantity + modifiedQuantity
                    : effectiveLineQuantity - modifiedQuantity;
            lineItem.updateEffectiveQuantity(newQuantity);
        }
    }

    public enum DeliveryMethod {
        // không vận chuyển
        none,
        // bán tại cửa hàng bán lẻ (offline)
        retail,
        // nhận tại cửa hàng (mua online, nhận offline)
        pick_up,
        // đối tác vc tích hợp
        external_service,
        // đối tác vc ngoài
        outside_shipper,
        // shipper ngoài
        external_shipper,
        // use "external_shipper" instead
        @Deprecated internal_shipper,
        // nhân viên cửa hàng
        employee,
        // sàn TMĐT
        ecommerce,
        //
        fulfillment_service,
        // -no longer used-
        @Deprecated shipping
    }

    public enum FulfillStatus {
        pending, open, success, cancelled, failure, confirmed, error
    }

    public enum ShipmentStatus {
        pending, delivering, delivered, returning, returned, cancelled, failed, retry_delivery, ready_to_pick, picked_up
    }

    public enum EffectQuantityType {
        add, subtract
    }
}
