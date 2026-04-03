package com.epam.finaltask.controller;

import com.epam.finaltask.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private static final String SESSION_DISPLAY_NAME = "displayName";

    private final UserRepository userRepository;

    @ModelAttribute(SESSION_DISPLAY_NAME)
    public String displayName(Principal principal, HttpSession session) {
        if (principal == null) {
            return null;
        }

        String cached = (String) session.getAttribute(SESSION_DISPLAY_NAME);
        if (cached != null) {
            return cached;
        }

        String name = userRepository.findUserByUsername(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .map(user -> user.getFirstName() != null ? user.getFirstName() : user.getUsername())
                .orElse(principal.getName());

        session.setAttribute(SESSION_DISPLAY_NAME, name);
        return name;
    }
}
