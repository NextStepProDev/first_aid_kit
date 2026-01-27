package com.firstaidkit.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

public class NotBeforeCurrentYearValidator implements ConstraintValidator<NotBeforeCurrentYear, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) return true;
        int currentYear = Year.now().getValue();
        return value >= currentYear;
    }
}