package com.epam.finaltask.controller.api;

import com.epam.finaltask.config.AppProperties;
import com.epam.finaltask.config.JwtService;
import com.epam.finaltask.config.OAuth2UserService;
import com.epam.finaltask.config.SecurityConfig;
import com.epam.finaltask.dto.response.VoucherDTO;
import com.epam.finaltask.exception.ResourceNotFoundException;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiVoucherController.class)
@Import({SecurityConfig.class, AppProperties.class})
@DisplayName("ApiVoucherController")
class ApiVoucherControllerTest {

    private static final UUID VOUCHER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VoucherService voucherService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private OAuth2UserService oAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private MessageSource messageSource;

    @Test
    @DisplayName("GET /api/v1/vouchers returns paginated JSON list")
    void getVouchers() throws Exception {
        VoucherDTO voucher = VoucherDTO.builder().id(VOUCHER_ID).title("Beach Tour").build();
        when(voucherService.findFiltered(any(), any())).thenReturn(new PageImpl<>(List.of(voucher)));

        mockMvc.perform(get("/api/v1/vouchers"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(VOUCHER_ID.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Beach Tour"));
    }

    @Test
    @DisplayName("GET /api/v1/vouchers with filter params")
    void getVouchersWithFilters() throws Exception {
        when(voucherService.findFiltered(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/vouchers")
                        .param("tourType", "ADVENTURE")
                        .param("minPrice", "100")
                        .param("maxPrice", "500"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/vouchers/{id} returns single voucher JSON")
    void getVoucherById() throws Exception {
        VoucherDTO voucher = VoucherDTO.builder().id(VOUCHER_ID).title("Mountain Trek").price(299.0).build();
        when(voucherService.getById(VOUCHER_ID)).thenReturn(voucher);

        mockMvc.perform(get("/api/v1/vouchers/{id}", VOUCHER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(VOUCHER_ID.toString()))
                .andExpect(jsonPath("$.title").value("Mountain Trek"))
                .andExpect(jsonPath("$.price").value(299.0));
    }

    @Test
    @DisplayName("GET /api/v1/vouchers/{id} not found returns JSON 404")
    void getVoucherNotFound() throws Exception {
        when(voucherService.getById(VOUCHER_ID)).thenThrow(new ResourceNotFoundException("Voucher not found"));

        mockMvc.perform(get("/api/v1/vouchers/{id}", VOUCHER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
