package com.epam.finaltask.dto.request;

import com.epam.finaltask.validation.PasswordConfirmable;
import com.epam.finaltask.validation.PasswordMatch;
import com.epam.finaltask.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class RegisterDTO implements PasswordConfirmable {

    @NotBlank(message = "{validation.user.username.required}")
    @Size(min = 3, max = 50, message = "{validation.user.username.size}")
    private String username;

    @NotBlank(message = "{validation.user.password.required}")
    @StrongPassword
    private String password;

    @NotBlank(message = "{validation.user.firstName.required}")
    @Size(max = 50, message = "{validation.user.firstName.size}")
    private String firstName;

    @NotBlank(message = "{validation.user.lastName.required}")
    @Size(max = 50, message = "{validation.user.lastName.size}")
    private String lastName;

    @NotBlank(message = "{validation.user.email.required}")
    @Email(regexp = ".+@.+\\..+", message = "{validation.user.email.invalid}")
    private String email;

    @Pattern(regexp = "^$|^\\+?\\d{10,15}$", message = "{validation.user.phone.pattern}")
    private String phoneNumber;

    private String confirmPassword;
}
