package com.epam.finaltask.validation.impl;

import com.epam.finaltask.dto.request.VoucherCreateDTO;
import com.epam.finaltask.validation.DateRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<DateRange, VoucherCreateDTO> {

    @Override
    public boolean isValid(VoucherCreateDTO dto, ConstraintValidatorContext context) {
        if (dto.getArrivalDate() == null || dto.getEvictionDate() == null) {
            return true; // let @NotNull handle nulls
        }

        boolean valid = dto.getEvictionDate().isAfter(dto.getArrivalDate());

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("evictionDate")
                    .addConstraintViolation();
        }

        return valid;
    }
}
