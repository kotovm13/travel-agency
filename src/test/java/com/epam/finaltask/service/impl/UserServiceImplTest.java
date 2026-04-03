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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDTO testUserDTO;
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .password("encodedPassword")
                .role(Role.USER)
                .phoneNumber("+380501234567")
                .balance(BigDecimal.valueOf(1000))
                .active(true)
                .build();

        testUserDTO = UserDTO.builder()
                .id(USER_ID)
                .username(USERNAME)
                .role("USER")
                .phoneNumber("+380501234567")
                .balance(BigDecimal.valueOf(1000))
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should create user successfully")
        void success() {
            RegisterDTO request = RegisterDTO.builder()
                    .username("newuser")
                    .password("Password1!")
                    .phoneNumber("+380509999999")
                    .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            UserDTO result = userService.createUser(request);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(USERNAME);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateUsernameException when username exists")
        void duplicateUsername() {
            RegisterDTO request = RegisterDTO.builder()
                    .username(USERNAME)
                    .password("Password1!")
                    .build();

            when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateUsernameException.class)
                    .hasMessageContaining("error.user.duplicate");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserByUsername")
    class GetUserByUsername {

        @Test
        @DisplayName("should return user when found")
        void success() {
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            UserDTO result = userService.getUserByUsername(USERNAME);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when not found")
        void notFound() {
            when(userRepository.findUserByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByUsername("unknown"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user when found")
        void success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            UserDTO result = userService.getUserById(USER_ID);

            assertThat(result.getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when not found")
        void notFound() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(unknownId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("should update phone number only")
        void phoneOnly() {
            UserUpdateDTO request = UserUpdateDTO.builder()
                    .phoneNumber("+380501111111")
                    .build();

            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userService.updateProfile(USERNAME, request);

            assertThat(testUser.getPhoneNumber()).isEqualTo("+380501111111");
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("should update password only")
        void passwordOnly() {
            UserUpdateDTO request = UserUpdateDTO.builder()
                    .password("NewPassword1!")
                    .build();

            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("NewPassword1!")).thenReturn("newEncodedPassword");
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userService.updateProfile(USERNAME, request);

            assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("should not change anything when all fields null")
        void nullFields() {
            UserUpdateDTO request = UserUpdateDTO.builder().build();
            String originalPhone = testUser.getPhoneNumber();
            String originalPassword = testUser.getPassword();

            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userService.updateProfile(USERNAME, request);

            assertThat(testUser.getPhoneNumber()).isEqualTo(originalPhone);
            assertThat(testUser.getPassword()).isEqualTo(originalPassword);
        }

        @Test
        @DisplayName("should not update password when blank")
        void blankPassword() {
            UserUpdateDTO request = UserUpdateDTO.builder()
                    .password("   ")
                    .build();

            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userService.updateProfile(USERNAME, request);

            assertThat(testUser.getPassword()).isEqualTo("encodedPassword");
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    @Nested
    @DisplayName("topUpBalance")
    class TopUpBalance {

        @Test
        @DisplayName("should add amount to balance")
        void success() {
            TopUpDTO request = TopUpDTO.builder()
                    .amount(BigDecimal.valueOf(500))
                    .build();

            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

            userService.topUpBalance(USERNAME, request);

            assertThat(testUser.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found")
        void userNotFound() {
            TopUpDTO request = TopUpDTO.builder()
                    .amount(BigDecimal.valueOf(500))
                    .build();

            when(userRepository.findUserByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.topUpBalance("unknown", request))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }
}
