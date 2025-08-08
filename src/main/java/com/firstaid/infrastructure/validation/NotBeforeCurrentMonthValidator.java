package com.firstaid.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class NotBeforeCurrentMonthValidator implements ConstraintValidator<NotBeforeCurrentMonth, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) return true;
        int currentMonth = LocalDate.now().getMonthValue();
        return value >= currentMonth;
    }
}
