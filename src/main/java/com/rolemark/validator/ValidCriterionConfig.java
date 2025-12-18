package com.rolemark.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CriterionConfigValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCriterionConfig {
    String message() default "Invalid criterion config";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

