package com.epam.finaltask.validation.impl;

import com.epam.finaltask.validation.PasswordConfirmable;
import com.epam.finaltask.validation.PasswordMatch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, PasswordConfirmable> {

    @Override
    public boolean isValid(PasswordConfirmable dto, ConstraintValidatorContext context) {
        String password = dto.getPassword();
        String confirmPassword = dto.getConfirmPassword();

        if (password == null && confirmPassword == null) {
            return true;
        }

        if (password == null || !password.equals(confirmPassword)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
