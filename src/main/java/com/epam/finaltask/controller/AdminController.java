package com.epam.finaltask.controller;

import com.epam.finaltask.dto.request.ChangeRoleDTO;
import com.epam.finaltask.model.enums.Role;
import com.epam.finaltask.service.StatsService;
import com.epam.finaltask.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final int PAGE_SIZE = 15;
    private static final String REDIRECT_USERS = "redirect:/admin/users";
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_SEARCH = "search";
    private static final String ATTR_ROLE_FILTER = "roleFilter";
    private static final String ATTR_STATUS_FILTER = "statusFilter";

    private final UserManagementService userManagementService;
    private final StatsService statsService;
    private final MessageSource messageSource;

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String search,
                        @RequestParam(required = false) String roleFilter,
                        @RequestParam(required = false) String statusFilter,
                        @RequestParam(defaultValue = "0") int page,
                        java.security.Principal principal,
                        Model model) {
        model.addAttribute("users", userManagementService.getAllUsers(search, roleFilter, statusFilter, PageRequest.of(page, PAGE_SIZE)));
        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute(ATTR_SEARCH, search);
        model.addAttribute(ATTR_ROLE_FILTER, roleFilter);
        model.addAttribute(ATTR_STATUS_FILTER, statusFilter);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/block")
    public String blockUser(@PathVariable UUID id,
                            java.security.Principal principal,
                            RedirectAttributes redirectAttributes) {
        userManagementService.blockUser(id, principal.getName());
        redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.user.blocked"));
        return REDIRECT_USERS;
    }

    @PostMapping("/users/{id}/unblock")
    public String unblockUser(@PathVariable UUID id,
                              java.security.Principal principal,
                              RedirectAttributes redirectAttributes) {
        userManagementService.unblockUser(id, principal.getName());
        redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.user.unblocked"));
        return REDIRECT_USERS;
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable UUID id,
                             @RequestParam String role,
                             java.security.Principal principal,
                             RedirectAttributes redirectAttributes) {
        userManagementService.changeRole(id, ChangeRoleDTO.builder().role(role).build(), principal.getName());
        redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.role.changed"));
        return REDIRECT_USERS;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", statsService.getStats());
        return "admin/dashboard";
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
