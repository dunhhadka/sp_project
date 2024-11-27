package org.example.order.order.domain.draftorder.model;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DraftOrderTag {
    @NotBlank
    @Size(max = 250)
    private String value;
    @Size(max = 250)
    private String alias;

    public DraftOrderTag(String tag) {
        this.value = tag;
        this.alias = tag;
    }
}
