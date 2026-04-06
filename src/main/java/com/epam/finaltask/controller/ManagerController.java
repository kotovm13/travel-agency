package com.epam.finaltask.controller;

import com.epam.finaltask.dto.request.ChangeStatusDTO;
import com.epam.finaltask.dto.request.VoucherCreateDTO;
import com.epam.finaltask.dto.response.VoucherDTO;
import com.epam.finaltask.exception.InvalidOrderStatusException;
import com.epam.finaltask.model.enums.BookingStatus;
import com.epam.finaltask.model.enums.HotelType;
import com.epam.finaltask.model.enums.TourType;
import com.epam.finaltask.model.enums.TransferType;
import com.epam.finaltask.service.OrderService;
import com.epam.finaltask.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController extends BaseController {

    private static final String REDIRECT_VOUCHERS = "redirect:/manager/vouchers";
    private static final String REDIRECT_ORDERS = "redirect:/manager/orders";
    private static final String VIEW_VOUCHER_FORM = "manager/voucher-form";
    private static final String VIEW_VOUCHER_LIST = "manager/voucher-list";
    private static final String VIEW_ORDERS = "manager/orders";
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_VOUCHER_FORM = "voucherForm";
    private static final String ATTR_VOUCHER_ID = "voucherId";
    private static final String ATTR_IS_EDIT = "isEdit";
    private static final String ATTR_VOUCHERS = "vouchers";
    private static final String ATTR_ORDERS = "orders";
    private static final String ATTR_SEARCH = "search";
    private static final String ATTR_DATE_FILTER = "dateFilter";
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_STATUS_FILTER = "statusFilter";

    private final VoucherService voucherService;
    private final OrderService orderService;

    @GetMapping("/vouchers")
    public String voucherList(@RequestParam(required = false) String search,
                              @RequestParam(required = false) String dateFilter,
                              @RequestParam(required = false) String tourType,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        model.addAttribute(ATTR_VOUCHERS, voucherService.findAllForManager(search, dateFilter, tourType, PageRequest.of(page, appProperties.getPagination().getManagerPageSize())));
        model.addAttribute(ATTR_SEARCH, search);
        model.addAttribute(ATTR_DATE_FILTER, dateFilter);
        model.addAttribute("tourTypeFilter", tourType);
        model.addAttribute("tourTypes", TourType.values());
        return VIEW_VOUCHER_LIST;
    }

    @GetMapping("/vouchers/new")
    public String createForm(Model model) {
        model.addAttribute(ATTR_VOUCHER_FORM, new VoucherCreateDTO());
        model.addAttribute(ATTR_IS_EDIT, false);
        addEnumsToModel(model);
        return VIEW_VOUCHER_FORM;
    }

    @PostMapping("/vouchers/new")
    public String create(@Valid @ModelAttribute(ATTR_VOUCHER_FORM) VoucherCreateDTO voucherForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_IS_EDIT, false);
            addEnumsToModel(model);
            return VIEW_VOUCHER_FORM;
        }

        voucherService.create(voucherForm);
        redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.voucher.created"));
        return REDIRECT_VOUCHERS;
    }

    @GetMapping("/vouchers/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        VoucherDTO voucher = voucherService.getById(id);
        VoucherCreateDTO form = VoucherCreateDTO.builder()
                .title(voucher.getTitle())
                .description(voucher.getDescription())
                .price(voucher.getPrice())
                .tourType(voucher.getTourType())
                .transferType(voucher.getTransferType())
                .hotelType(voucher.getHotelType())
                .arrivalDate(voucher.getArrivalDate())
                .evictionDate(voucher.getEvictionDate())
                .hot(voucher.isHot())
                .discount(voucher.getDiscount())
                .quantity(voucher.getQuantity())
                .build();

        model.addAttribute(ATTR_VOUCHER_FORM, form);
        model.addAttribute(ATTR_VOUCHER_ID, id);
        model.addAttribute(ATTR_IS_EDIT, true);
        addEnumsToModel(model);
        return VIEW_VOUCHER_FORM;
    }

    @PostMapping("/vouchers/{id}/edit")
    public String update(@PathVariable UUID id,
                         @Valid @ModelAttribute(ATTR_VOUCHER_FORM) VoucherCreateDTO voucherForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_VOUCHER_ID, id);
            model.addAttribute(ATTR_IS_EDIT, true);
            addEnumsToModel(model);
            return VIEW_VOUCHER_FORM;
        }

        try {
            voucherService.update(id, voucherForm);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.voucher.updated"));
            return REDIRECT_VOUCHERS;
        } catch (InvalidOrderStatusException e) {
            model.addAttribute(ATTR_VOUCHER_ID, id);
            model.addAttribute(ATTR_IS_EDIT, true);
            model.addAttribute(ATTR_ERROR, getMessage(e.getMessageKey(), e.getArgs()));
            addEnumsToModel(model);
            return VIEW_VOUCHER_FORM;
        }
    }

    @PostMapping("/vouchers/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            voucherService.delete(id);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.voucher.deleted"));
        } catch (InvalidOrderStatusException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, getMessage(e.getMessageKey(), e.getArgs()));
        }
        return REDIRECT_VOUCHERS;
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String search,
                         @RequestParam(required = false) String username,
                         @RequestParam(required = false) String statusFilter,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        model.addAttribute(ATTR_ORDERS, orderService.getAllOrders(search, username, statusFilter, PageRequest.of(page, appProperties.getPagination().getManagerPageSize())));
        model.addAttribute(ATTR_SEARCH, search);
        model.addAttribute(ATTR_USERNAME, username);
        model.addAttribute(ATTR_STATUS_FILTER, statusFilter);
        model.addAttribute("bookingStatuses", BookingStatus.values());
        return VIEW_ORDERS;
    }

    @PostMapping("/orders/{id}/status")
    public String changeStatus(@PathVariable UUID id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.changeStatus(id, ChangeStatusDTO.builder().status(status).build());
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, getMessage("success.status.changed"));
        } catch (InvalidOrderStatusException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, getMessage(e.getMessageKey(), e.getArgs()));
        }
        return REDIRECT_ORDERS;
    }

    private void addEnumsToModel(Model model) {
        model.addAttribute("tourTypes", TourType.values());
        model.addAttribute("transferTypes", TransferType.values());
        model.addAttribute("hotelTypes", HotelType.values());
    }

}
