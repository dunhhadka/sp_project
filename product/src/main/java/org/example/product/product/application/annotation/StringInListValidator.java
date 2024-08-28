package org.example.product.product.application.annotation;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class StringInListValidator implements ConstraintValidator<StringInList, String> {

    private StringInList annotation;

    @Override
    public void initialize(StringInList stringInList) {
        this.annotation = stringInList;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (this.annotation.allowBlank() && StringUtils.isEmpty(value))
            return true;
        else if (ArrayUtils.contains(this.annotation.array(), value))
            return true;

        var message = String.format("is not in [%s]",
                StringUtils.join(annotation.array(), ", "));
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
        return false;
    }
}
