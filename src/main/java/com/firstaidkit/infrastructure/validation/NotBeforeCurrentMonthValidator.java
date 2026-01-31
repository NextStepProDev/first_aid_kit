package com.firstaidkit.infrastructure.validation;

import com.firstaidkit.controller.dto.drug.DrugCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class NotBeforeCurrentMonthValidator implements ConstraintValidator<NotBeforeCurrentMonth, DrugCreateRequest> {

    @Override
    public boolean isValid(DrugCreateRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        Integer year = request.getExpirationYear();
        Integer month = request.getExpirationMonth();

        if (year == null || month == null) return true;

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        if (year > currentYear) return true;
        if (year == currentYear) return month >= currentMonth;
        return false;
    }
}
