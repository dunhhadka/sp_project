package org.example.order.order.domain.fulfillmentorder.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

@Getter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedLocation {
    @Size(max = 128)
    private String name;
    @Size(max = 25)
    private String phone;
    @Size(max = 128)
    private String email;
    @Size(max = 255)
    private String address;
}
