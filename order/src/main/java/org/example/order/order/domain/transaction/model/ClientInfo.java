package org.example.order.order.domain.transaction.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;

import javax.validation.constraints.Size;

@Getter
@Embeddable
public class ClientInfo {
    private Integer userId;
    @Size(max = 64)
    private String clientId;
    @Size(max = 50)
    private String deviceId;
}
