package com.epam.finaltask.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String phoneNumber;
    private BigDecimal balance;
    private boolean active;
}
