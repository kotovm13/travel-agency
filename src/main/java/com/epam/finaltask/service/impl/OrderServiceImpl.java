package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.request.ChangeStatusDTO;
import com.epam.finaltask.dto.response.BookingDTO;
import com.epam.finaltask.exception.*;
import com.epam.finaltask.mapper.BookingMapper;
import com.epam.finaltask.model.Booking;
import com.epam.finaltask.model.User;
import com.epam.finaltask.model.Voucher;
import com.epam.finaltask.model.enums.BookingStatus;
import com.epam.finaltask.model.enums.VoucherStatus;
import com.epam.finaltask.repository.BookingRepository;
import com.epam.finaltask.repository.BookingSpecification;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherRepository;
import com.epam.finaltask.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.epam.finaltask.util.ErrorConstants.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String VOUCHER_NOT_AVAILABLE_KEY = "error.order.not.available";
    private static final String VOUCHER_SOLD_OUT_KEY = "error.order.no.quantity";
    private static final String INSUFFICIENT_BALANCE_KEY = "error.balance.insufficient";
    private static final String BOOKING_NOT_OWNED_KEY = "error.order.not.owned";
    private static final String CANCEL_ONLY_REGISTERED_KEY = "error.order.cancel.only.registered";
    private static final String STATUS_CHANGE_ONLY_REGISTERED_KEY = "error.order.status.only.registered";
    private static final String TOUR_ALREADY_STARTED_KEY = "error.order.tour.started";

    private final BookingRepository bookingRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingDTO orderVoucher(UUID voucherId, String username) {
        Voucher voucher = findVoucherById(voucherId);
        User user = findUserByUsername(username);

        if (voucher.getStatus() != VoucherStatus.AVAILABLE) {
            throw new InvalidOrderStatusException(VOUCHER_NOT_AVAILABLE_KEY);
        }

        if (voucher.getArrivalDate() != null && !voucher.getArrivalDate().isAfter(java.time.LocalDate.now())) {
            throw new InvalidOrderStatusException(TOUR_ALREADY_STARTED_KEY);
        }

        long activeBookings = bookingRepository.countActiveBookingsByVoucherId(voucherId);
        if (activeBookings >= voucher.getQuantity()) {
            throw new InvalidOrderStatusException(VOUCHER_SOLD_OUT_KEY);
        }

        BigDecimal discountedPrice = calculateDiscountedPrice(voucher);

        if (user.getBalance().compareTo(discountedPrice) < 0) {
            throw new InsufficientBalanceException(INSUFFICIENT_BALANCE_KEY, discountedPrice, user.getBalance());
        }

        user.setBalance(user.getBalance().subtract(discountedPrice));
        userRepository.save(user);

        Booking booking = Booking.builder()
                .user(user)
                .voucher(voucher)
                .status(BookingStatus.REGISTERED)
                .bookedPrice(discountedPrice)
                .build();

        return bookingMapper.toBookingDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDTO cancelOrder(UUID bookingId, String username) {
        Booking booking = findBookingById(bookingId);
        User user = findUserByUsername(username);

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new InvalidOrderStatusException(BOOKING_NOT_OWNED_KEY);
        }

        if (booking.getStatus() != BookingStatus.REGISTERED) {
            throw new InvalidOrderStatusException(CANCEL_ONLY_REGISTERED_KEY);
        }

        user.setBalance(user.getBalance().add(booking.getBookedPrice()));
        userRepository.save(user);

        booking.setStatus(BookingStatus.CANCELED);

        return bookingMapper.toBookingDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public BookingDTO changeStatus(UUID bookingId, ChangeStatusDTO request) {
        Booking booking = findBookingById(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new InvalidOrderStatusException(STATUS_CHANGE_ONLY_REGISTERED_KEY);
        }

        BookingStatus newStatus = BookingStatus.valueOf(request.getStatus());

        if (newStatus == BookingStatus.CANCELED) {
            User user = booking.getUser();
            user.setBalance(user.getBalance().add(booking.getBookedPrice()));
            userRepository.save(user);
        }

        booking.setStatus(newStatus);
        return bookingMapper.toBookingDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingDTO> getUserOrders(String username, String status, Pageable pageable) {
        User user = findUserByUsername(username);

        if (status != null && !status.isBlank()) {
            return bookingRepository.findAllByUserIdAndStatus(user.getId(), BookingStatus.valueOf(status), pageable)
                    .map(bookingMapper::toBookingDTO);
        }

        return bookingRepository.findAllByUserId(user.getId(), pageable)
                .map(bookingMapper::toBookingDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<BookingDTO> getAllOrders(String search, String username, String status, Pageable pageable) {
        Specification<Booking> spec = Specification.where(null);

        if (hasValue(search)) {
            spec = spec.and(BookingSpecification.voucherTitleContains(search));
        }
        if (hasValue(username)) {
            spec = spec.and(BookingSpecification.usernameContains(username));
        }
        if (hasValue(status)) {
            spec = spec.and(BookingSpecification.hasStatus(status));
        }

        return bookingRepository.findAll(spec, pageable).map(bookingMapper::toBookingDTO);
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal calculateDiscountedPrice(Voucher voucher) {
        BigDecimal price = BigDecimal.valueOf(voucher.getPrice());
        if (voucher.getDiscount() != null && voucher.getDiscount() > 0) {
            BigDecimal discountFactor = BigDecimal.valueOf(100L - voucher.getDiscount())
                    .divide(BigDecimal.valueOf(100));
            price = price.multiply(discountFactor);
        }
        return price;
    }

    private Voucher findVoucherById(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new VoucherNotFoundException(VOUCHER_NOT_FOUND_ID + id));
    }

    private Booking findBookingById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_ID + id));
    }

    private User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_USERNAME + username));
    }
}
