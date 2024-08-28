package org.example.order.order.application.model.order.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderRoutingResponse {
    private List<OrderRoutingResult> orderRoutingResults;

    @Getter
    @Builder
    public static class OrderRoutingResult {
        private OrderRoutingLocation location;
        private List<IndexesItem> indexesItems;
    }

    @Getter
    @Builder
    public static class OrderRoutingLocation {
        private Long id;
        private Integer storeId;
        private String code;
        private String name;
        private String email;
        private String phone;
        private String country;
        private String countryCode;
        private String province;
        private String provinceCode;
        private String district;
        private String districtCode;
        private String ward;
        private String wardCode;
        private String address;
        private String zipCode;
    }

    @Getter
    @Builder
    public static class IndexesItem {
        private Integer index;
        private Integer variantId;
        private String name;
        private Integer inventoryItemId;
    }
}
