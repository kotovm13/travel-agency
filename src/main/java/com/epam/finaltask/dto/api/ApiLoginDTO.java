package com.epam.finaltask.dto.api;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiLoginDTO {

    @NotBlank(message = "{validation.user.username.required}")
    private String username;

    @NotBlank(message = "{validation.user.password.required}")
    private String password;
}
