package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.request.RegisterDTO;
import com.epam.finaltask.service.AuthenticationService;
import com.epam.finaltask.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserService userService;

    @Override
    @Transactional
    public void register(RegisterDTO request) {
        userService.createUser(request);
    }
}
