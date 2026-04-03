package com.epam.finaltask.service;

import com.epam.finaltask.dto.request.ChangeRoleDTO;
import com.epam.finaltask.dto.response.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserManagementService {

    Page<UserDTO> getAllUsers(String search, String role, String status, Pageable pageable);

    UserDTO blockUser(UUID id, String currentUsername);

    UserDTO unblockUser(UUID id, String currentUsername);

    UserDTO changeRole(UUID id, ChangeRoleDTO request, String currentUsername);

    UserDTO getUserById(UUID id);

    void resetPassword(UUID id, String currentUsername);
}
