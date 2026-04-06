package com.epam.finaltask.validation;

import com.epam.finaltask.validation.impl.PasswordMatchValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {

    String message() default "{validation.user.password.mismatch}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
