package org.example.order.order.infrastructure.data.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private long id;
    private int storeId;
    private String code;
    private String name;
    private String email;
    private String phone;
    private String address;
    private boolean isDefaultLocation;
    private boolean inventoryManagement;
}
