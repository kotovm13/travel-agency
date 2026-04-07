package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.response.StatsDTO;
import com.epam.finaltask.model.enums.BookingStatus;
import com.epam.finaltask.model.enums.VoucherStatus;

import java.math.BigDecimal;
import java.util.Optional;
import com.epam.finaltask.repository.BookingRepository;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherRepository;
import com.epam.finaltask.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class StatsServiceImpl implements StatsService {

    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public StatsDTO getStats() {
        return StatsDTO.builder()
                .totalUsers(userRepository.countAllUsers())
                .activeUsers(userRepository.countActiveUsers())
                .availableVouchers(voucherRepository.countByStatus(VoucherStatus.AVAILABLE))
                .registeredOrders(bookingRepository.countByStatus(BookingStatus.REGISTERED))
                .paidOrders(bookingRepository.countByStatus(BookingStatus.PAID))
                .canceledOrders(bookingRepository.countByStatus(BookingStatus.CANCELED))
                .totalRevenue(Optional.ofNullable(bookingRepository.sumPaidRevenue())
                        .orElse(BigDecimal.ZERO).doubleValue())
                .build();
    }
}
