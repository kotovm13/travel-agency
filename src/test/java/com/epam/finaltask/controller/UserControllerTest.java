package com.epam.finaltask.controller;

import com.epam.finaltask.config.SecurityConfig;
import com.epam.finaltask.dto.response.UserDTO;
import com.epam.finaltask.service.OrderService;
import com.epam.finaltask.service.UserService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private OrderService orderService;

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

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /profile returns profile page")
    void profile() throws Exception {
        UserDTO user = UserDTO.builder().username("testuser").balance(BigDecimal.valueOf(1000)).build();
        when(userService.getUserByUsername("testuser")).thenReturn(user);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().attributeExists("user", "updateDTO"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /profile/topup redirects to profile")
    void topUp() throws Exception {
        when(userService.topUpBalance(eq("testuser"), any())).thenReturn(null);

        mockMvc.perform(post("/profile/topup").with(csrf())
                        .param("amount", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /profile/update success redirects")
    void updateProfileSuccess() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(UserDTO.builder().build());
        when(userService.updateProfile(eq("testuser"), any())).thenReturn(null);

        mockMvc.perform(post("/profile/update").with(csrf())
                        .param("phoneNumber", "+380501234567"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /profile/update validation error returns form")
    void updateProfileValidationError() throws Exception {
        UserDTO user = UserDTO.builder().username("testuser").build();
        when(userService.getUserByUsername("testuser")).thenReturn(user);

        mockMvc.perform(post("/profile/update").with(csrf())
                        .param("phoneNumber", "invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /my-vouchers returns bookings")
    void myVouchers() throws Exception {
        when(orderService.getUserOrders(eq("testuser"), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/my-vouchers"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/my-vouchers"))
                .andExpect(model().attributeExists("bookings"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /my-vouchers/{id}/cancel redirects")
    void cancelOrder() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(orderService.cancelOrder(any(), eq("testuser"))).thenReturn(null);

        mockMvc.perform(post("/my-vouchers/{id}/cancel", bookingId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-vouchers"));
    }

    @Test
    @DisplayName("GET /profile unauthenticated redirects to login")
    void profileUnauthenticated() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
