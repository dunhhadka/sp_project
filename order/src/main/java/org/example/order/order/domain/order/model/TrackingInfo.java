package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import javax.validation.constraints.Size;

@Getter
@Builder
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TrackingInfo {
    @Size(max = 50)
    private String source;
    @Size(max = 50)
    private String sourceName;
    @Size(max = 32)
    private String cartToken;
    @Size(max = 32)
    private String checkoutToken;
    @Size(max = 2000)
    private String landingSite;
    @Size(max = 2000)
    private String reference;
    @Size(max = 255)
    private String sourceIdentifier;
    @Size(max = 255)
    private String sourceUrl;
}
