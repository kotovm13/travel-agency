package com.epam.finaltask.controller.api;

import com.epam.finaltask.config.JwtService;
import com.epam.finaltask.dto.api.ApiLoginDTO;
import com.epam.finaltask.dto.api.ApiTokenDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

import static com.epam.finaltask.util.PathConstants.PATH_API_V1_AUTH;

@RestController
@RequestMapping(PATH_API_V1_AUTH)
@RequiredArgsConstructor
@Tag(name = "Authentication API", description = "Get JWT token for API access")
public class ApiAuthController {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_TIMESTAMP = "timestamp";

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final MessageSource messageSource;

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token", description = "Authenticate with username/password, receive JWT token for API requests")
    public ResponseEntity<?> login(@Valid @RequestBody ApiLoginDTO loginDTO) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsername(),
                            loginDTO.getPassword())
            );
        } catch (AuthenticationException ex) {
            String message = messageSource.getMessage(
                    "error.auth.bad.credentials", null, LocaleContextHolder.getLocale());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    FIELD_STATUS, HttpStatus.UNAUTHORIZED.value(),
                    FIELD_ERROR, message,
                    FIELD_TIMESTAMP, Instant.now().toString()
            ));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.getUsername());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new ApiTokenDTO(token));
    }
}
