package com.drugs.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValueOfEnumValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueOfEnum {

    Class<? extends Enum<?>> enumClass();
    String message() default "must be any of {enumValues}";
    @SuppressWarnings("unused")
    Class<?>[] groups() default {};
    @SuppressWarnings("unused")
    Class<? extends Payload>[] payload() default {};
}