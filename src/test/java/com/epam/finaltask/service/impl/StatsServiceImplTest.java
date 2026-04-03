package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.response.StatsDTO;
import com.epam.finaltask.model.enums.BookingStatus;
import com.epam.finaltask.model.enums.VoucherStatus;
import com.epam.finaltask.repository.BookingRepository;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsServiceImpl")
class StatsServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    @Test
    @DisplayName("should return all metrics")
    void getStats() {
        when(userRepository.countAllUsers()).thenReturn(10L);
        when(userRepository.countActiveUsers()).thenReturn(8L);
        when(voucherRepository.countByStatus(VoucherStatus.AVAILABLE)).thenReturn(20L);
        when(bookingRepository.countByStatus(BookingStatus.REGISTERED)).thenReturn(5L);
        when(bookingRepository.countByStatus(BookingStatus.PAID)).thenReturn(3L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELED)).thenReturn(2L);
        when(bookingRepository.sumPaidRevenue()).thenReturn(BigDecimal.valueOf(4500));

        StatsDTO result = statsService.getStats();

        assertThat(result.getTotalUsers()).isEqualTo(10);
        assertThat(result.getActiveUsers()).isEqualTo(8);
        assertThat(result.getAvailableVouchers()).isEqualTo(20);
        assertThat(result.getRegisteredOrders()).isEqualTo(5);
        assertThat(result.getPaidOrders()).isEqualTo(3);
        assertThat(result.getCanceledOrders()).isEqualTo(2);
        assertThat(result.getTotalRevenue()).isEqualTo(4500.0);
    }

    @Test
    @DisplayName("should return zeros when no data")
    void emptyStats() {
        when(userRepository.countAllUsers()).thenReturn(0L);
        when(userRepository.countActiveUsers()).thenReturn(0L);
        when(voucherRepository.countByStatus(VoucherStatus.AVAILABLE)).thenReturn(0L);
        when(bookingRepository.countByStatus(BookingStatus.REGISTERED)).thenReturn(0L);
        when(bookingRepository.countByStatus(BookingStatus.PAID)).thenReturn(0L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELED)).thenReturn(0L);
        when(bookingRepository.sumPaidRevenue()).thenReturn(BigDecimal.ZERO);

        StatsDTO result = statsService.getStats();

        assertThat(result.getTotalUsers()).isZero();
        assertThat(result.getTotalRevenue()).isZero();
    }
}
