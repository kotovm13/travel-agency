package com.epam.finaltask.controller;

import com.epam.finaltask.config.SecurityConfig;
import com.epam.finaltask.dto.response.VoucherDTO;
import com.epam.finaltask.exception.InsufficientBalanceException;
import com.epam.finaltask.exception.ResourceNotFoundException;
import com.epam.finaltask.service.OrderService;
import com.epam.finaltask.service.VoucherService;
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

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogueController.class)
@Import({SecurityConfig.class, com.epam.finaltask.config.AppProperties.class})
@DisplayName("CatalogueController")
class CatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VoucherService voucherService;

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

    private static final UUID VOUCHER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        when(messageSource.getMessage(any(String.class), any(), any(Locale.class))).thenReturn("message");
    }

    @Test
    @DisplayName("GET /catalogue returns list with model attributes")
    void catalogue() throws Exception {
        when(voucherService.findFiltered(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/catalogue"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogue/list"))
                .andExpect(model().attributeExists("vouchers", "filter", "tourTypes", "transferTypes", "hotelTypes"));
    }

    @Test
    @DisplayName("GET /catalogue with filter params")
    void catalogueWithFilters() throws Exception {
        when(voucherService.findFiltered(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/catalogue")
                        .param("tourType", "ADVENTURE")
                        .param("hotelType", "FOUR_STARS"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogue/list"));
    }

    @Test
    @DisplayName("GET /catalogue/{id} returns detail")
    void detail() throws Exception {
        VoucherDTO voucher = VoucherDTO.builder().id(VOUCHER_ID).title("Test").build();
        when(voucherService.getById(VOUCHER_ID)).thenReturn(voucher);

        mockMvc.perform(get("/catalogue/{id}", VOUCHER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogue/detail"))
                .andExpect(model().attributeExists("voucher"));
    }

    @Test
    @DisplayName("GET /catalogue/{id} not found returns error page")
    void detailNotFound() throws Exception {
        when(voucherService.getById(VOUCHER_ID)).thenThrow(new ResourceNotFoundException("Voucher not found"));

        mockMvc.perform(get("/catalogue/{id}", VOUCHER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /catalogue/{id}/order redirects to my-vouchers")
    void orderSuccess() throws Exception {
        when(orderService.orderVoucher(any(), any())).thenReturn(
                com.epam.finaltask.dto.response.BookingDTO.builder().id(UUID.randomUUID()).build());

        mockMvc.perform(post("/catalogue/{id}/order", VOUCHER_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-vouchers"));

        verify(orderService).orderVoucher(any(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /catalogue/{id}/order insufficient balance redirects with error")
    void orderInsufficientBalance() throws Exception {
        when(orderService.orderVoucher(any(), any()))
                .thenThrow(new InsufficientBalanceException("error.balance.insufficient"));

        mockMvc.perform(post("/catalogue/{id}/order", VOUCHER_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /catalogue/{id}/order without CSRF is forbidden")
    void orderWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/catalogue/{id}/order", VOUCHER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /catalogue unauthenticated is allowed")
    void catalogueUnauthenticated() throws Exception {
        when(voucherService.findFiltered(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/catalogue"))
                .andExpect(status().isOk());
    }
}
