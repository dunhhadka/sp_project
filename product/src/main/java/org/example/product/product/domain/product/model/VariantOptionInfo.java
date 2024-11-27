package org.example.product.product.domain.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.example.product.ddd.ValueObject;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantOptionInfo extends ValueObject<VariantOptionInfo> {
    public static final String DEFAULT_OPTION_VALUE = "Default Title";
    @NotBlank
    @Builder.Default
    @Size(max = 500)
    private String option1 = DEFAULT_OPTION_VALUE;

    @Size(max = 500)
    private String option2;

    @Size(max = 500)
    private String option3;


    public String getTitle() {
        var titleBuilder = new StringBuilder(option1);
        if (StringUtils.isNoneBlank(this.option2)) {
            titleBuilder.append(" / ").append(option2);
        }
        if (StringUtils.isNoneBlank(this.option3)) {
            titleBuilder.append(" / ").append(option3);
        }
        return titleBuilder.toString();
    }
}
