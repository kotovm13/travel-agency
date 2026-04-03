package com.epam.finaltask.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoleDTO {

    @NotNull(message = "{validation.user.role.required}")
    @Pattern(regexp = "USER|MANAGER|ADMIN", message = "{validation.user.role.invalid}")
    private String role;
}
