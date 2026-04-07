package com.epam.finaltask.controller;

import com.epam.finaltask.dto.request.ChangeRoleDTO;
import com.epam.finaltask.model.enums.Role;
import com.epam.finaltask.service.StatsService;
import com.epam.finaltask.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController extends BaseController {

    private static final String REDIRECT_USERS = "redirect:/admin/users";
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_SEARCH = "search";
    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_ROLE_FILTER = "roleFilter";
    private static final String ATTR_STATUS_FILTER = "statusFilter";

    private final UserManagementService userManagementService;
    private final StatsService statsService;

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String search,
                        @RequestParam(required = false) String email,
                        @RequestParam(required = false) String roleFilter,
                        @RequestParam(required = false) String statusFilter,
                        @RequestParam(defaultValue = "0") int page,
                        java.security.Principal principal,
                        Model model) {
        model.addAttribute("users", userManagementService.getAllUsers(search, email, roleFilter, statusFilter, PageRequest.of(page, appProperties.getPagination().getAdminPageSize())));
        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute(ATTR_SEARCH, search);
        model.addAttribute(ATTR_EMAIL, email);
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

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable UUID id,
                           java.security.Principal principal,
                           Model model) {
        model.addAttribute("user", userManagementService.getUserById(id));
        model.addAttribute("currentUsername", principal.getName());
        return "admin/user-detail";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable UUID id,
                                java.security.Principal principal,
                                RedirectAttributes redirectAttributes) {
        userManagementService.resetPassword(id, principal.getName());
        redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.password.reset"));
        return "redirect:/admin/users/" + id;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", statsService.getStats());
        return "admin/dashboard";
    }
}
