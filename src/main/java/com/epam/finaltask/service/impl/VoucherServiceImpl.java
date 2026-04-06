package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.request.VoucherCreateDTO;
import com.epam.finaltask.dto.request.VoucherFilterDTO;
import com.epam.finaltask.dto.response.VoucherDTO;
import com.epam.finaltask.exception.InvalidOrderStatusException;
import com.epam.finaltask.exception.VoucherNotFoundException;
import com.epam.finaltask.mapper.VoucherMapper;
import com.epam.finaltask.model.Voucher;
import com.epam.finaltask.model.enums.VoucherStatus;
import com.epam.finaltask.repository.BookingRepository;
import com.epam.finaltask.repository.VoucherRepository;
import com.epam.finaltask.repository.VoucherSpecification;
import com.epam.finaltask.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.epam.finaltask.util.ErrorConstants.VOUCHER_NOT_FOUND_ID;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private static final String QUANTITY_BELOW_BOOKINGS_KEY = "error.voucher.quantity.below.bookings";
    private static final String PRICE_RANGE_INVALID_KEY = "error.filter.price.range.invalid";
    private static final String DELETE_ACTIVE_BOOKINGS_KEY = "error.voucher.delete.active.bookings";
    private static final String DATE_FILTER_EXPIRED = "expired";
    private static final String DATE_FILTER_ACTIVE = "active";
    private static final String SORT_PRICE_ASC = "price_asc";
    private static final String SORT_PRICE_DESC = "price_desc";
    private static final String SORT_DISCOUNT_DESC = "discount_desc";
    private static final String FIELD_PRICE = "price";
    private static final String FIELD_DISCOUNT = "discount";
    private static final String FIELD_IS_HOT = "isHot";

    private final VoucherRepository voucherRepository;
    private final BookingRepository bookingRepository;
    private final VoucherMapper voucherMapper;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public VoucherDTO create(VoucherCreateDTO request) {
        Voucher voucher = voucherMapper.toVoucher(request);
        return toEnrichedDTO(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public VoucherDTO update(UUID id, VoucherCreateDTO request) {
        Voucher voucher = findVoucherById(id);
        long activeBookings = bookingRepository.countActiveBookingsByVoucherId(id);
        if (request.getQuantity() < activeBookings) {
            throw new InvalidOrderStatusException(QUANTITY_BELOW_BOOKINGS_KEY, activeBookings);
        }
        voucherMapper.updateVoucherFromDTO(request, voucher);
        return toEnrichedDTO(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void delete(UUID id) {
        if (!voucherRepository.existsById(id)) {
            throw new VoucherNotFoundException(VOUCHER_NOT_FOUND_ID + id);
        }
        long activeBookings = bookingRepository.countActiveBookingsByVoucherId(id);
        if (activeBookings > 0) {
            throw new InvalidOrderStatusException(DELETE_ACTIVE_BOOKINGS_KEY, activeBookings);
        }
        voucherRepository.deleteById(id);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public VoucherDTO markHot(UUID id, boolean hot) {
        Voucher voucher = findVoucherById(id);
        voucher.setHot(hot);
        return toEnrichedDTO(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public VoucherDTO setDiscount(UUID id, Integer discount) {
        Voucher voucher = findVoucherById(id);
        voucher.setDiscount(discount);
        return toEnrichedDTO(voucherRepository.save(voucher));
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherDTO getById(UUID id) {
        return toEnrichedDTO(findVoucherById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherDTO> findFiltered(VoucherFilterDTO filter, Pageable pageable) {
        Specification<Voucher> spec = VoucherSpecification.hasStatus(VoucherStatus.AVAILABLE)
                .and(VoucherSpecification.arrivalDateAfterToday());

        if (hasValue(filter.getTourType())) {
            spec = spec.and(VoucherSpecification.hasTourType(filter.getTourType()));
        }
        if (hasValue(filter.getTransferType())) {
            spec = spec.and(VoucherSpecification.hasTransferType(filter.getTransferType()));
        }
        if (hasValue(filter.getHotelType())) {
            spec = spec.and(VoucherSpecification.hasHotelType(filter.getHotelType()));
        }
        if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
            if (filter.getMinPrice() != null && filter.getMaxPrice() != null && filter.getMinPrice() > filter.getMaxPrice()) {
                throw new InvalidOrderStatusException(PRICE_RANGE_INVALID_KEY);
            }
            spec = spec.and(VoucherSpecification.priceBetween(filter.getMinPrice(), filter.getMaxPrice()));
        }
        if (hasValue(filter.getSearch())) {
            spec = spec.and(VoucherSpecification.titleContains(filter.getSearch()));
        }

        Pageable sortedPageable = applySorting(pageable, filter.getSort());

        return toEnrichedPage(voucherRepository.findAll(spec, sortedPageable));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<VoucherDTO> findAllForManager(String search, String dateFilter, String tourType, Pageable pageable) {
        Specification<Voucher> spec = Specification.where(null);

        if (hasValue(search)) {
            spec = spec.and(VoucherSpecification.titleContains(search));
        }
        if (DATE_FILTER_ACTIVE.equals(dateFilter)) {
            spec = spec.and(VoucherSpecification.arrivalDateAfterToday());
        } else if (DATE_FILTER_EXPIRED.equals(dateFilter)) {
            spec = spec.and(VoucherSpecification.arrivalDateBeforeOrEqualToday());
        }
        if (hasValue(tourType)) {
            spec = spec.and(VoucherSpecification.hasTourType(tourType));
        }

        return toEnrichedPage(voucherRepository.findAll(spec, pageable));
    }

    private Pageable applySorting(Pageable pageable, String sort) {
        Sort sorting;
        if (SORT_PRICE_ASC.equals(sort)) {
            sorting = Sort.by(FIELD_PRICE).ascending();
        } else if (SORT_PRICE_DESC.equals(sort)) {
            sorting = Sort.by(FIELD_PRICE).descending();
        } else if (SORT_DISCOUNT_DESC.equals(sort)) {
            sorting = Sort.by(FIELD_DISCOUNT).descending();
        } else {
            sorting = Sort.by(FIELD_IS_HOT).descending().and(Sort.by(FIELD_PRICE).ascending());
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sorting);
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private Page<VoucherDTO> toEnrichedPage(Page<Voucher> page) {
        List<UUID> ids = page.getContent().stream().map(Voucher::getId).toList();
        Map<UUID, Long> countsMap = bookingRepository.countActiveBookingsByVoucherIds(ids).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

        return page.map(voucher -> {
            VoucherDTO dto = voucherMapper.toVoucherDTO(voucher);
            long activeBookings = countsMap.getOrDefault(voucher.getId(), 0L);
            dto.setAvailableQuantity(voucher.getQuantity() - activeBookings);
            return dto;
        });
    }

    private VoucherDTO toEnrichedDTO(Voucher voucher) {
        VoucherDTO dto = voucherMapper.toVoucherDTO(voucher);
        long activeBookings = bookingRepository.countActiveBookingsByVoucherId(voucher.getId());
        dto.setAvailableQuantity(voucher.getQuantity() - activeBookings);
        return dto;
    }

    private Voucher findVoucherById(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new VoucherNotFoundException(VOUCHER_NOT_FOUND_ID + id));
    }
}
