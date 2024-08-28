package org.example.order.order.domain.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class OrderTag {
    @Column(name = "tag")
    private @NotBlank @Size(max = 250) String value;
    @Column(name = "tagAlias")
    private @Size(max = 255) String alias;
}
