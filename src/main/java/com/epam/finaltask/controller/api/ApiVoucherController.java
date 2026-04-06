package com.epam.finaltask.controller.api;

import com.epam.finaltask.config.AppProperties;
import com.epam.finaltask.dto.request.VoucherFilterDTO;
import com.epam.finaltask.dto.response.VoucherDTO;
import com.epam.finaltask.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.epam.finaltask.util.PathConstants.PATH_API_V1_VOUCHERS;

@RestController
@RequestMapping(PATH_API_V1_VOUCHERS)
@RequiredArgsConstructor
@Tag(name = "Voucher API", description = "Public API for available tours — for third-party integrations and ads")
public class ApiVoucherController {

    private final VoucherService voucherService;
    private final AppProperties appProperties;

    @GetMapping
    @Operation(summary = "Get available vouchers", description = "Returns paginated list of available tours with optional filtering and sorting")
    public Page<VoucherDTO> getVouchers(
            @Parameter(description = "Tour type filter") @RequestParam(required = false) String tourType,
            @Parameter(description = "Transfer type filter") @RequestParam(required = false) String transferType,
            @Parameter(description = "Hotel type filter") @RequestParam(required = false) String hotelType,
            @Parameter(description = "Min price filter") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Max price filter") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "Search by title") @RequestParam(required = false) String search,
            @Parameter(description = "Sort: price_asc, price_desc, discount_desc") @RequestParam(required = false) String sort,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        int pageSize = size != null ? size : appProperties.getPagination().getApiPageSize();

        VoucherFilterDTO filter = VoucherFilterDTO.builder()
                .tourType(tourType)
                .transferType(transferType)
                .hotelType(hotelType)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .search(search)
                .sort(sort)
                .build();

        return voucherService.findFiltered(filter, PageRequest.of(page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get voucher by ID", description = "Returns a single voucher with availability info")
    public VoucherDTO getVoucher(@PathVariable UUID id) {
        return voucherService.getById(id);
    }
}
