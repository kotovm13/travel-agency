package com.epam.finaltask.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.Map;

import static com.epam.finaltask.util.PathConstants.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_TIMESTAMP = "timestamp";

    private final AuthenticationProvider authenticationProvider;
    private final BlockedUserFilter blockedUserFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2UserService oAuth2UserService;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PATH_API + "/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PATH_API_V1_AUTH + "/**").permitAll()
                        .requestMatchers(PATH_API_V1_VOUCHERS + "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "error.auth.required"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "error.access.denied"))
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider)
                .addFilterAfter(blockedUserFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", PATH_CATALOGUE, PATH_CATALOGUE + "/**",
                                PATH_LOGIN, PATH_REGISTER,
                                PATH_CSS + "/**", PATH_JS + "/**", PATH_IMAGES + "/**",
                                PATH_ERROR, PATH_H2_CONSOLE + "/**",
                                PATH_SWAGGER_UI + "/**", PATH_API_DOCS + "/**").permitAll()
                        .requestMatchers(PATH_MANAGER + "/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(PATH_ADMIN + "/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage(PATH_LOGIN)
                        .defaultSuccessUrl(PATH_CATALOGUE, true)
                        .failureUrl(PATH_LOGIN + "?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage(PATH_LOGIN)
                        .defaultSuccessUrl(PATH_CATALOGUE, true)
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                )
                .logout(logout -> logout
                        .logoutUrl(PATH_LOGOUT)
                        .logoutSuccessUrl(PATH_LOGIN + "?logout=true")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage(PATH_ERROR + "/403")
                );

        http.headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, int status, String messageKey)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = messageSource.getMessage(messageKey, null, messageKey, LocaleContextHolder.getLocale());
        Map<String, Object> body = Map.of(
                FIELD_STATUS, status,
                FIELD_ERROR, message,
                FIELD_TIMESTAMP, Instant.now().toString()
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
