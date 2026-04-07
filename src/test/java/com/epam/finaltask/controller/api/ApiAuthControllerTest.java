package com.epam.finaltask.controller.api;

import com.epam.finaltask.config.AppProperties;
import com.epam.finaltask.config.JwtService;
import com.epam.finaltask.config.OAuth2UserService;
import com.epam.finaltask.config.SecurityConfig;
import com.epam.finaltask.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiAuthController.class)
@Import({SecurityConfig.class, AppProperties.class})
@DisplayName("ApiAuthController")
class ApiAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private OAuth2UserService oAuth2UserService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private MessageSource messageSource;

    @Test
    @DisplayName("POST /api/v1/auth/login success returns token")
    void loginSuccess() throws Exception {
        UserDetails userDetails = User.withUsername("testuser")
                .password("encoded")
                .roles("USER")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("test-jwt-token");

        String body = objectMapper.writeValueAsString(Map.of("username", "testuser", "password", "Password1!"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("test-jwt-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login bad credentials returns JSON 401")
    void loginBadCredentials() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(messageSource.getMessage(eq("error.auth.bad.credentials"), any(), any(Locale.class)))
                .thenReturn("Invalid username or password");

        String body = objectMapper.writeValueAsString(Map.of("username", "wrong", "password", "wrong"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Invalid username or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login empty body returns JSON 400")
    void loginEmptyBody() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "", "password", ""));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login missing body returns JSON error")
    void loginNoBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
