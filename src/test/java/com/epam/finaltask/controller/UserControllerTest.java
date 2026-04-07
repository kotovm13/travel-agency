package com.epam.finaltask.controller;

import com.epam.finaltask.config.SecurityConfig;
import com.epam.finaltask.dto.request.TopUpDTO;
import com.epam.finaltask.dto.request.UserUpdateDTO;
import com.epam.finaltask.dto.response.BookingDTO;
import com.epam.finaltask.dto.response.UserDTO;
import com.epam.finaltask.exception.DuplicateUsernameException;
import com.epam.finaltask.exception.InvalidOrderStatusException;
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

import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, com.epam.finaltask.config.AppProperties.class})
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
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        when(messageSource.getMessage(any(String.class), any(), any(Locale.class))).thenReturn("message");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /profile returns profile page with user data")
    void profile() throws Exception {
        when(userService.getUserByUsername("testuser"))
                .thenReturn(UserDTO.builder().username("testuser").balance(BigDecimal.valueOf(1000)).build());

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().attributeExists("user", "updateDTO"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /profile/topup adds balance and redirects")
    void topUpSuccess() throws Exception {
        when(userService.topUpBalance(eq("testuser"), any(TopUpDTO.class)))
                .thenReturn(UserDTO.builder().username("testuser").balance(BigDecimal.valueOf(1500)).build());

        mockMvc.perform(post("/profile/topup").with(csrf())
                        .param("amount", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).topUpBalance(eq("testuser"), any(TopUpDTO.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /profile/topup with negative amount shows error")
    void topUpNegativeAmount() throws Exception {
        mockMvc.perform(post("/profile/topup").with(csrf())
                        .param("amount", "-100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /profile/topup with amount exceeding max shows error")
    void topUpExceedsMax() throws Exception {
        mockMvc.perform(post("/profile/topup").with(csrf())
                        .param("amount", "999999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /profile/update updates profile and redirects")
    void updateProfileSuccess() throws Exception {
        when(userService.getUserByUsername("testuser"))
                .thenReturn(UserDTO.builder().username("testuser").build());
        when(userService.updateProfile(eq("testuser"), any(UserUpdateDTO.class)))
                .thenReturn(UserDTO.builder().username("testuser").phoneNumber("+380501111111").build());

        mockMvc.perform(post("/profile/update").with(csrf())
                        .param("phoneNumber", "+380501111111"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).updateProfile(eq("testuser"), any(UserUpdateDTO.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /profile/update with duplicate email shows error")
    void updateProfileDuplicateEmail() throws Exception {
        when(userService.getUserByUsername("testuser"))
                .thenReturn(UserDTO.builder().username("testuser").build());
        when(userService.updateProfile(eq("testuser"), any(UserUpdateDTO.class)))
                .thenThrow(new DuplicateUsernameException("error.user.email.duplicate"));

        mockMvc.perform(post("/profile/update").with(csrf())
                        .param("email", "taken@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /my-vouchers returns bookings page")
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
    @DisplayName("GET /my-vouchers with status filter")
    void myVouchersWithStatusFilter() throws Exception {
        when(orderService.getUserOrders(eq("testuser"), eq("REGISTERED"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/my-vouchers").param("status", "REGISTERED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("status", "REGISTERED"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /my-vouchers/{id}/cancel cancels booking and redirects")
    void cancelOrderSuccess() throws Exception {
        when(orderService.cancelOrder(BOOKING_ID, "testuser"))
                .thenReturn(BookingDTO.builder().id(BOOKING_ID).status("CANCELED").build());

        mockMvc.perform(post("/my-vouchers/{id}/cancel", BOOKING_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-vouchers"))
                .andExpect(flash().attributeExists("success"));

        verify(orderService).cancelOrder(BOOKING_ID, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /my-vouchers/{id}/cancel already canceled shows error")
    void cancelOrderAlreadyCanceled() throws Exception {
        when(orderService.cancelOrder(BOOKING_ID, "testuser"))
                .thenThrow(new InvalidOrderStatusException("error.order.already.canceled"));

        mockMvc.perform(post("/my-vouchers/{id}/cancel", BOOKING_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-vouchers"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /profile unauthenticated redirects to login")
    void profileUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection());
    }
}
