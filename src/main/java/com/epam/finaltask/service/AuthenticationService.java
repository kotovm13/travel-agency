package com.epam.finaltask.service;

import com.epam.finaltask.dto.request.RegisterDTO;

public interface AuthenticationService {
    void register(RegisterDTO request);
}
