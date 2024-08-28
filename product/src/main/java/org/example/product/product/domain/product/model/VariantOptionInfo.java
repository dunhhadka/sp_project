package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VariantOptionInfo {
    public static final String DEFAULT_OPTION_VARIANT = "Default Title";
    @Size(max = 500)
    private String option1 = DEFAULT_OPTION_VARIANT;
    @Size(max = 500)
    private String option2;
    @Size(max = 500)
    private String option3;

    public String getTitle() {
        var titleBuilder = new StringBuilder(this.option1);
        if (StringUtils.isNotBlank(this.option2)) {
            titleBuilder = titleBuilder.append(" / ").append(this.option2);
        }
        if (StringUtils.isNotBlank(this.option3)) {
            titleBuilder = titleBuilder.append(" / ").append(this.option3);
        }
        return titleBuilder.toString();
    }
}
