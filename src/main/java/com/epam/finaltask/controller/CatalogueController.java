package com.epam.finaltask.controller;

import com.epam.finaltask.dto.request.VoucherFilterDTO;
import com.epam.finaltask.exception.InsufficientBalanceException;
import com.epam.finaltask.exception.InvalidOrderStatusException;
import com.epam.finaltask.exception.LocalizedException;
import com.epam.finaltask.model.enums.HotelType;
import com.epam.finaltask.model.enums.TourType;
import com.epam.finaltask.model.enums.TransferType;
import com.epam.finaltask.service.OrderService;
import com.epam.finaltask.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CatalogueController {

    private static final int PAGE_SIZE = 9;
    private static final String ATTR_VOUCHERS = "vouchers";
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_ERROR = "error";
    private static final String REDIRECT_CATALOGUE = "redirect:/catalogue/";

    private final VoucherService voucherService;
    private final OrderService orderService;
    private final MessageSource messageSource;

    @GetMapping({"/catalogue", "/"})
    public String catalogue(@ModelAttribute VoucherFilterDTO filter,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        try {
            model.addAttribute(ATTR_VOUCHERS, voucherService.findFiltered(filter, PageRequest.of(page, PAGE_SIZE)));
        } catch (InvalidOrderStatusException e) {
            model.addAttribute(ATTR_VOUCHERS, org.springframework.data.domain.Page.empty());
            model.addAttribute(ATTR_ERROR, getMessage(e.getMessageKey(), e.getArgs()));
        }
        model.addAttribute("filter", filter);
        model.addAttribute("tourTypes", TourType.values());
        model.addAttribute("transferTypes", TransferType.values());
        model.addAttribute("hotelTypes", HotelType.values());

        return "catalogue/list";
    }

    @GetMapping("/catalogue/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("voucher", voucherService.getById(id));
        return "catalogue/detail";
    }

    @PostMapping("/catalogue/{id}/order")
    public String order(@PathVariable UUID id,
                        java.security.Principal principal,
                        RedirectAttributes redirectAttributes) {
        try {
            orderService.orderVoucher(id, principal.getName());
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.order.placed"));
            return "redirect:/my-vouchers";
        } catch (InsufficientBalanceException | InvalidOrderStatusException e) {
            LocalizedException le = e;
            redirectAttributes.addFlashAttribute(ATTR_ERROR, getMessage(le.getMessageKey(), le.getArgs()));
            return REDIRECT_CATALOGUE + id;
        }
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
