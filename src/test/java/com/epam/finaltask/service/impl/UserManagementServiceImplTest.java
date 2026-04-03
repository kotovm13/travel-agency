package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.request.ChangeRoleDTO;
import com.epam.finaltask.dto.response.UserDTO;
import com.epam.finaltask.exception.UserNotFoundException;
import com.epam.finaltask.mapper.UserMapper;
import com.epam.finaltask.model.User;
import com.epam.finaltask.model.enums.Role;
import com.epam.finaltask.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementServiceImpl")
class UserManagementServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private com.epam.finaltask.config.BlockedUserFilter blockedUserFilter;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    private User testUser;
    private UserDTO testUserDTO;
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ADMIN_USERNAME = "admin";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username("testuser")
                .role(Role.USER)
                .balance(BigDecimal.valueOf(1000))
                .active(true)
                .build();

        testUserDTO = UserDTO.builder()
                .id(USER_ID)
                .username("testuser")
                .role("USER")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("should return paginated user list")
        void success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(testUser));

            when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            Page<UserDTO> result = userManagementService.getAllUsers(null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty page when no users")
        void empty() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(Page.empty());

            Page<UserDTO> result = userManagementService.getAllUsers(null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("blockUser")
    class BlockUser {

        @Test
        @DisplayName("should deactivate user account")
        void success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userManagementService.blockUser(USER_ID, ADMIN_USERNAME);

            assertThat(testUser.isActive()).isFalse();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should throw when trying to block self")
        void cannotBlockSelf() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userManagementService.blockUser(USER_ID, "testuser"))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("should throw when user not found")
        void notFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.blockUser(USER_ID, ADMIN_USERNAME))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(USER_ID.toString());
        }
    }

    @Nested
    @DisplayName("unblockUser")
    class UnblockUser {

        @Test
        @DisplayName("should activate user account")
        void success() {
            testUser.setActive(false);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userManagementService.unblockUser(USER_ID, ADMIN_USERNAME);

            assertThat(testUser.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("changeRole")
    class ChangeRole {

        @Test
        @DisplayName("should change user role to MANAGER")
        void toManager() {
            ChangeRoleDTO request = ChangeRoleDTO.builder().role("MANAGER").build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userManagementService.changeRole(USER_ID, request, ADMIN_USERNAME);

            assertThat(testUser.getRole()).isEqualTo(Role.MANAGER);
        }

        @Test
        @DisplayName("should change user role to ADMIN")
        void toAdmin() {
            ChangeRoleDTO request = ChangeRoleDTO.builder().role("ADMIN").build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userManagementService.changeRole(USER_ID, request, ADMIN_USERNAME);

            assertThat(testUser.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("should throw when trying to change own role")
        void cannotChangeOwnRole() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            ChangeRoleDTO request = ChangeRoleDTO.builder().role("MANAGER").build();

            assertThatThrownBy(() -> userManagementService.changeRole(USER_ID, request, "testuser"))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("should throw when user not found")
        void notFound() {
            ChangeRoleDTO request = ChangeRoleDTO.builder().role("MANAGER").build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.changeRole(USER_ID, request, ADMIN_USERNAME))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(USER_ID.toString());
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should block already active user (idempotent)")
        void blockActiveUser() {
            assertThat(testUser.isActive()).isTrue();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userManagementService.blockUser(USER_ID, ADMIN_USERNAME);

            assertThat(testUser.isActive()).isFalse();
        }

        @Test
        @DisplayName("should unblock already active user (idempotent)")
        void unblockActiveUser() {
            assertThat(testUser.isActive()).isTrue();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userManagementService.unblockUser(USER_ID, ADMIN_USERNAME);

            assertThat(testUser.isActive()).isTrue();
        }
    }
}
