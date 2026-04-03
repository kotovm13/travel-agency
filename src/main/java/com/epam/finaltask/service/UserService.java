package com.epam.finaltask.service;

import com.epam.finaltask.dto.request.RegisterDTO;
import com.epam.finaltask.dto.request.TopUpDTO;
import com.epam.finaltask.dto.request.UserUpdateDTO;
import com.epam.finaltask.dto.response.UserDTO;

import java.util.UUID;

public interface UserService {

    UserDTO createUser(RegisterDTO request);

    UserDTO getUserByUsername(String username);

    UserDTO getUserById(UUID id);

    UserDTO updateProfile(String username, UserUpdateDTO request);

    UserDTO topUpBalance(String username, TopUpDTO request);
}
