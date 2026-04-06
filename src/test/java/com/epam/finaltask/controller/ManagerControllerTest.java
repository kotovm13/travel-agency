package com.epam.finaltask.controller;

import com.epam.finaltask.config.SecurityConfig;
import com.epam.finaltask.dto.response.VoucherDTO;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerController.class)
@Import({SecurityConfig.class, com.epam.finaltask.config.AppProperties.class})
@DisplayName("ManagerController")
class ManagerControllerTest {

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
    private static final UUID VOUCHER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /manager/vouchers returns list")
    void voucherList() throws Exception {
        when(voucherService.findAllForManager(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/manager/vouchers"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/voucher-list"))
                .andExpect(model().attributeExists("vouchers", "tourTypes"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /manager/vouchers/new returns form")
    void createForm() throws Exception {
        mockMvc.perform(get("/manager/vouchers/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/voucher-form"))
                .andExpect(model().attributeExists("voucherForm", "isEdit"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /manager/vouchers/new success redirects")
    void createSuccess() throws Exception {
        when(voucherService.create(any())).thenReturn(VoucherDTO.builder().build());

        mockMvc.perform(post("/manager/vouchers/new").with(csrf())
                        .param("title", "Test Tour")
                        .param("description", "Description")
                        .param("price", "1500")
                        .param("tourType", "ADVENTURE")
                        .param("transferType", "PLANE")
                        .param("hotelType", "FOUR_STARS")
                        .param("arrivalDate", "2027-06-01")
                        .param("evictionDate", "2027-06-10")
                        .param("quantity", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/vouchers"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /manager/vouchers/{id}/edit returns form")
    void editForm() throws Exception {
        VoucherDTO voucher = VoucherDTO.builder()
                .id(VOUCHER_ID).title("Test").price(1000.0)
                .tourType("ADVENTURE").transferType("PLANE").hotelType("FOUR_STARS")
                .discount(0).quantity(10).build();
        when(voucherService.getById(VOUCHER_ID)).thenReturn(voucher);

        mockMvc.perform(get("/manager/vouchers/{id}/edit", VOUCHER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/voucher-form"))
                .andExpect(model().attributeExists("voucherForm", "voucherId", "isEdit"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /manager/vouchers/{id}/delete redirects")
    void delete() throws Exception {
        mockMvc.perform(post("/manager/vouchers/{id}/delete", VOUCHER_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/vouchers"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /manager/orders returns orders")
    void orders() throws Exception {
        when(orderService.getAllOrders(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/manager/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/orders"))
                .andExpect(model().attributeExists("orders", "bookingStatuses"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /manager/vouchers as USER is forbidden")
    void forbiddenForUser() throws Exception {
        mockMvc.perform(get("/manager/vouchers"))
                .andExpect(status().isForbidden());
    }
}
