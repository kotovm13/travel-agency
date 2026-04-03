package com.epam.finaltask.dto.request;

import com.epam.finaltask.validation.PasswordConfirmable;
import com.epam.finaltask.validation.PasswordMatch;
import com.epam.finaltask.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@PasswordMatch
public class UserUpdateDTO implements PasswordConfirmable {

    @Size(max = 50, message = "{validation.user.firstName.size}")
    private String firstName;

    @Size(max = 50, message = "{validation.user.lastName.size}")
    private String lastName;

    @Email(regexp = "^$|.+@.+\\..+", message = "{validation.user.email.invalid}")
    private String email;

    @Pattern(regexp = "^$|^\\+?\\d{10,15}$", message = "{validation.user.phone.pattern}")
    private String phoneNumber;

    @StrongPassword
    private String password;

    private String confirmPassword;
}
