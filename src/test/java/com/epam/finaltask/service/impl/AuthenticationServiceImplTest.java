package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.request.RegisterDTO;
import com.epam.finaltask.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationServiceImpl")
class AuthenticationServiceImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    @DisplayName("should delegate registration to UserService")
    void register() {
        RegisterDTO request = RegisterDTO.builder()
                .username("newuser")
                .password("Password1!")
                .phoneNumber("+380509999999")
                .build();

        authenticationService.register(request);

        verify(userService).createUser(request);
    }
}
