package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.request.RegisterDTO;
import com.epam.finaltask.dto.request.TopUpDTO;
import com.epam.finaltask.dto.request.UserUpdateDTO;
import com.epam.finaltask.dto.response.UserDTO;
import com.epam.finaltask.exception.DuplicateUsernameException;
import com.epam.finaltask.exception.UserNotFoundException;
import com.epam.finaltask.mapper.UserMapper;
import com.epam.finaltask.model.User;
import com.epam.finaltask.model.enums.Role;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.epam.finaltask.util.ErrorConstants.USER_NOT_FOUND_ID;
import static com.epam.finaltask.util.ErrorConstants.USER_NOT_FOUND_USERNAME;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USERNAME_ALREADY_EXISTS_KEY = "error.user.duplicate";
    private static final String EMAIL_ALREADY_EXISTS_KEY = "error.user.email.duplicate";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDTO createUser(RegisterDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException(USERNAME_ALREADY_EXISTS_KEY, request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUsernameException(EMAIL_ALREADY_EXISTS_KEY, request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(Role.USER)
                .phoneNumber(request.getPhoneNumber())
                .build();

        return userMapper.toUserDTO(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        User user = findUserByUsername(username);
        return userMapper.toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_ID + id));
        return userMapper.toUserDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateProfile(String username, UserUpdateDTO request) {
        User user = findUserByUsername(username);

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateUsernameException(EMAIL_ALREADY_EXISTS_KEY, request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userMapper.toUserDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDTO topUpBalance(String username, TopUpDTO request) {
        User user = findUserByUsername(username);
        user.setBalance(user.getBalance().add(request.getAmount()));
        return userMapper.toUserDTO(userRepository.save(user));
    }

    private User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_USERNAME + username));
    }
}
