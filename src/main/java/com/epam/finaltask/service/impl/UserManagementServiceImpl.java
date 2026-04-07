package com.epam.finaltask.service.impl;

import com.epam.finaltask.config.AppProperties;
import com.epam.finaltask.config.BlockedUserFilter;
import com.epam.finaltask.dto.request.ChangeRoleDTO;
import com.epam.finaltask.dto.response.UserDTO;
import com.epam.finaltask.exception.UserNotFoundException;
import com.epam.finaltask.mapper.UserMapper;
import com.epam.finaltask.model.User;
import com.epam.finaltask.model.enums.Role;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.UserSpecification;
import com.epam.finaltask.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.epam.finaltask.util.ErrorConstants.USER_NOT_FOUND_ID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementServiceImpl implements UserManagementService {

    private static final String CANNOT_MODIFY_SELF = "Cannot modify your own account";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_BLOCKED = "blocked";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final BlockedUserFilter blockedUserFilter;
    private final AppProperties appProperties;

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(String search, String email, String role, String status, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (hasValue(search)) {
            spec = spec.and(UserSpecification.usernameContains(search));
        }
        if (hasValue(email)) {
            spec = spec.and(UserSpecification.emailContains(email));
        }
        if (hasValue(role)) {
            spec = spec.and(UserSpecification.hasRole(role));
        }
        if (STATUS_ACTIVE.equals(status)) {
            spec = spec.and(UserSpecification.isActive(true));
        } else if (STATUS_BLOCKED.equals(status)) {
            spec = spec.and(UserSpecification.isActive(false));
        }

        return userRepository.findAll(spec, pageable).map(userMapper::toUserDTO);
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    @Transactional
    public UserDTO blockUser(UUID id, String currentUsername) {
        User user = findById(id);
        validateNotSelf(user, currentUsername);
        user.setActive(false);
        UserDTO result = userMapper.toUserDTO(userRepository.save(user));
        blockedUserFilter.evictUser(user.getUsername());
        return result;
    }

    @Override
    @Transactional
    public UserDTO unblockUser(UUID id, String currentUsername) {
        User user = findById(id);
        validateNotSelf(user, currentUsername);
        user.setActive(true);
        return userMapper.toUserDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDTO changeRole(UUID id, ChangeRoleDTO request, String currentUsername) {
        User user = findById(id);
        validateNotSelf(user, currentUsername);
        user.setRole(Role.valueOf(request.getRole()));
        return userMapper.toUserDTO(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        return userMapper.toUserDTO(findById(id));
    }

    @Override
    @Transactional
    public void resetPassword(UUID id, String currentUsername) {
        User user = findById(id);
        validateNotSelf(user, currentUsername);
        user.setPassword(passwordEncoder.encode(appProperties.getSecurity().getDefaultPassword()));
        userRepository.save(user);
    }

    private void validateNotSelf(User targetUser, String currentUsername) {
        if (targetUser.getUsername().equals(currentUsername)) {
            throw new AccessDeniedException(CANNOT_MODIFY_SELF);
        }
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_ID + id));
    }
}
