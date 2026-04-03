package com.epam.finaltask.controller;

import com.epam.finaltask.dto.request.TopUpDTO;
import com.epam.finaltask.dto.request.UserUpdateDTO;
import com.epam.finaltask.exception.DuplicateUsernameException;
import com.epam.finaltask.exception.InvalidOrderStatusException;
import com.epam.finaltask.service.OrderService;
import com.epam.finaltask.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserController {

    private static final int PAGE_SIZE = 10;
    private static final String REDIRECT_PROFILE = "redirect:/profile";
    private static final String REDIRECT_MY_VOUCHERS = "redirect:/my-vouchers";
    private static final String VIEW_PROFILE = "user/profile";
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_ERROR = "error";

    private final UserService userService;
    private final OrderService orderService;
    private final MessageSource messageSource;

    @GetMapping("/profile")
    public String profile(java.security.Principal principal, Model model) {
        model.addAttribute("user", userService.getUserByUsername(principal.getName()));
        if (!model.containsAttribute("updateDTO")) {
            model.addAttribute("updateDTO", new UserUpdateDTO());
        }
        return VIEW_PROFILE;
    }

    @PostMapping("/profile/topup")
    public String topUp(@RequestParam BigDecimal amount,
                        java.security.Principal principal,
                        RedirectAttributes redirectAttributes) {
        userService.topUpBalance(principal.getName(), TopUpDTO.builder().amount(amount).build());
        redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.topup"));
        return REDIRECT_PROFILE;
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("updateDTO") UserUpdateDTO updateDTO,
                                BindingResult bindingResult,
                                java.security.Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getUserByUsername(principal.getName()));
            return VIEW_PROFILE;
        }

        try {
            userService.updateProfile(principal.getName(), updateDTO);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.profile.updated"));
        } catch (DuplicateUsernameException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, getMessage(e.getMessageKey(), e.getArgs()));
        }
        return REDIRECT_PROFILE;
    }

    @GetMapping("/my-vouchers")
    public String myVouchers(java.security.Principal principal,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        model.addAttribute("bookings", orderService.getUserOrders(principal.getName(), status, PageRequest.of(page, PAGE_SIZE)));
        model.addAttribute("status", status);
        return "user/my-vouchers";
    }

    @PostMapping("/my-vouchers/{id}/cancel")
    public String cancelOrder(@PathVariable UUID id,
                              java.security.Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id, principal.getName());
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.order.canceled"));
        } catch (InvalidOrderStatusException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, getMessage(e.getMessageKey(), e.getArgs()));
        }
        return REDIRECT_MY_VOUCHERS;
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
