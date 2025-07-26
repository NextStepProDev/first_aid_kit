package com.drugs.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = NotBeforeCurrentYearValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface NotBeforeCurrentYear {
    @SuppressWarnings("unused")
    String message() default "Expiration year must be current year or later";

    @SuppressWarnings("unused")
    Class<?>[] groups() default {};

    @SuppressWarnings("unused")
    Class<? extends Payload>[] payload() default {};
}