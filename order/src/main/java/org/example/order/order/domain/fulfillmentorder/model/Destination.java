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
public class Destination {
    @Size(max = 50)
    protected String firstName;
    @Size(max = 50)
    protected String lastName;
    @Size(max = 25)
    protected String phone;
    @Size(max = 128)
    private String email;
    @Size(max = 255)
    private String address;
}
