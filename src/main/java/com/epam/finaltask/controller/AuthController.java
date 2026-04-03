package com.epam.finaltask.controller;

import com.epam.finaltask.dto.request.RegisterDTO;
import com.epam.finaltask.exception.DuplicateUsernameException;
import com.epam.finaltask.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private static final String AUTH_LOGIN = "auth/login";
    private static final String AUTH_REGISTER = "auth/register";

    private final AuthenticationService authenticationService;
    private final MessageSource messageSource;

    @GetMapping("/login")
    public String loginPage() {
        return AUTH_LOGIN;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return AUTH_REGISTER;
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return AUTH_REGISTER;
        }

        try {
            authenticationService.register(registerDTO);
            return "redirect:/login";
        } catch (DuplicateUsernameException e) {
            model.addAttribute("error", getMessage(e.getMessageKey(), e.getArgs()));
            return AUTH_REGISTER;
        }
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
