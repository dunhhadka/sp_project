package org.example.order.order.domain.fulfillment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.order.application.converter.StringListJoinAttributeConverter;

import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Embeddable
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrackingInfo {
    @Size(max = 128)
    private String trackingCompany;
    @Size(max = 20)
    private String carrier;
    @Size(max = 128)
    private String carrierName;

    @Column
    @Convert(converter = StringListJoinAttributeConverter.class)
    private List<@Size(max = 50) String> trackingNumbers;

    @Column
    @Convert(converter = StringListJoinAttributeConverter.class)
    private List<@Size(max = 255) String> trackingUrls;
}
