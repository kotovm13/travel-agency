package com.epam.finaltask.controller;

import com.epam.finaltask.config.SecurityConfig;
import com.epam.finaltask.dto.response.StatsDTO;
import com.epam.finaltask.dto.response.UserDTO;
import com.epam.finaltask.service.StatsService;
import com.epam.finaltask.service.UserManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, com.epam.finaltask.config.AppProperties.class})
@DisplayName("AdminController")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private MessageSource messageSource;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private com.epam.finaltask.repository.UserRepository userRepository;

    @MockitoBean
    private com.epam.finaltask.config.JwtService jwtService;

    @MockitoBean
    private com.epam.finaltask.config.OAuth2UserService oAuth2UserService;
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/users returns user list")
    void users() throws Exception {
        when(userManagementService.getAllUsers(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users", "currentUsername", "roles"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/users/{id}/block redirects")
    void blockUser() throws Exception {
        when(userManagementService.blockUser(any(), any())).thenReturn(UserDTO.builder().build());

        mockMvc.perform(post("/admin/users/{id}/block", USER_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/users/{id}/role redirects")
    void changeRole() throws Exception {
        when(userManagementService.changeRole(any(), any(), any())).thenReturn(UserDTO.builder().build());

        mockMvc.perform(post("/admin/users/{id}/role", USER_ID).with(csrf())
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/dashboard returns stats")
    void dashboard() throws Exception {
        when(statsService.getStats()).thenReturn(StatsDTO.builder().build());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("stats"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /admin/users as USER is forbidden")
    void forbiddenForUser() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /admin/users as MANAGER is forbidden")
    void forbiddenForManager() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }
}
