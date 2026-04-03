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
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Voucher availableVoucher;
    private Booking registeredBooking;
    private BookingDTO bookingDTO;

    private static final UUID VOUCHER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(USER_ID).username(USERNAME)
                .balance(BigDecimal.valueOf(3000)).active(true).build();

        availableVoucher = Voucher.builder().id(VOUCHER_ID).title("Test Tour")
                .price(1000.0).status(VoucherStatus.AVAILABLE).discount(0).quantity(10).build();

        registeredBooking = Booking.builder().id(BOOKING_ID).user(testUser)
                .voucher(availableVoucher).status(BookingStatus.REGISTERED)
                .bookedPrice(BigDecimal.valueOf(1000)).build();

        bookingDTO = BookingDTO.builder().id(BOOKING_ID).userId(USER_ID)
                .username(USERNAME).voucherId(VOUCHER_ID).voucherTitle("Test Tour")
                .bookedPrice(BigDecimal.valueOf(1000)).status("REGISTERED").build();
    }

    @Nested
    @DisplayName("orderVoucher")
    class OrderVoucher {

        @Test
        @DisplayName("should create booking and deduct balance")
        void success() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(0L);
            when(bookingRepository.save(any(Booking.class))).thenReturn(registeredBooking);
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            BookingDTO result = orderService.orderVoucher(VOUCHER_ID, USERNAME);

            assertThat(result).isNotNull();
            assertThat(testUser.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(2000));
            verify(userRepository).save(testUser);
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("should apply discount when ordering")
        void withDiscount() {
            availableVoucher.setDiscount(20);
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(0L);
            when(bookingRepository.save(any(Booking.class))).thenReturn(registeredBooking);
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            orderService.orderVoucher(VOUCHER_ID, USERNAME);

            assertThat(testUser.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(2200));
        }

        @Test
        @DisplayName("should throw when voucher is disabled")
        void notAvailable() {
            availableVoucher.setStatus(VoucherStatus.DISABLED);
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> orderService.orderVoucher(VOUCHER_ID, USERNAME))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }

        @Test
        @DisplayName("should throw when tour has already started")
        void tourAlreadyStarted() {
            availableVoucher.setArrivalDate(java.time.LocalDate.now().minusDays(1));
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> orderService.orderVoucher(VOUCHER_ID, USERNAME))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }

        @Test
        @DisplayName("should throw when tour starts today")
        void tourStartsToday() {
            availableVoucher.setArrivalDate(java.time.LocalDate.now());
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> orderService.orderVoucher(VOUCHER_ID, USERNAME))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }

        @Test
        @DisplayName("should throw when quantity is exhausted")
        void soldOut() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(10L);

            assertThatThrownBy(() -> orderService.orderVoucher(VOUCHER_ID, USERNAME))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }

        @Test
        @DisplayName("should throw when balance is insufficient")
        void insufficientBalance() {
            testUser.setBalance(BigDecimal.valueOf(500));
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(0L);

            assertThatThrownBy(() -> orderService.orderVoucher(VOUCHER_ID, USERNAME))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        @DisplayName("should throw when voucher not found")
        void voucherNotFound() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.orderVoucher(VOUCHER_ID, USERNAME))
                    .isInstanceOf(VoucherNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when user not found")
        void userNotFound() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.orderVoucher(VOUCHER_ID, "unknown"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("should order free tour with 100% discount")
        void fullDiscount() {
            availableVoucher.setDiscount(100);
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(0L);
            when(bookingRepository.save(any(Booking.class))).thenReturn(registeredBooking);
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            orderService.orderVoucher(VOUCHER_ID, USERNAME);

            assertThat(testUser.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        }

        @Test
        @DisplayName("should allow same user to order same voucher twice if quantity allows")
        void orderSameVoucherTwice() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(availableVoucher));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(1L);
            when(bookingRepository.save(any(Booking.class))).thenReturn(registeredBooking);
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            BookingDTO result = orderService.orderVoucher(VOUCHER_ID, USERNAME);

            assertThat(result).isNotNull();
            verify(bookingRepository).save(any(Booking.class));
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("should cancel booking and refund balance")
        void success() {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(registeredBooking));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.save(any(Booking.class))).thenReturn(registeredBooking);
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            orderService.cancelOrder(BOOKING_ID, USERNAME);

            assertThat(testUser.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(4000));
            assertThat(registeredBooking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        }

        @Test
        @DisplayName("should throw when booking belongs to another user")
        void notOwned() {
            User other = User.builder().id(UUID.randomUUID()).username("other").build();
            registeredBooking.setUser(other);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(registeredBooking));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> orderService.cancelOrder(BOOKING_ID, USERNAME))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }

        @Test
        @DisplayName("should throw when booking is not REGISTERED")
        void notRegistered() {
            registeredBooking.setStatus(BookingStatus.PAID);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(registeredBooking));
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> orderService.cancelOrder(BOOKING_ID, USERNAME))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }

        @Test
        @DisplayName("should throw when booking not found")
        void notFound() {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(BOOKING_ID, USERNAME))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("should change to PAID")
        void toPaid() {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(registeredBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(registeredBooking);
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            orderService.changeStatus(BOOKING_ID, ChangeStatusDTO.builder().status("PAID").build());

            assertThat(registeredBooking.getStatus()).isEqualTo(BookingStatus.PAID);
        }

        @Test
        @DisplayName("should cancel and refund on CANCELED")
        void toCanceled() {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(registeredBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(registeredBooking);
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            orderService.changeStatus(BOOKING_ID, ChangeStatusDTO.builder().status("CANCELED").build());

            assertThat(registeredBooking.getStatus()).isEqualTo(BookingStatus.CANCELED);
            assertThat(testUser.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(4000));
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should throw when already CANCELED")
        void notRegistered() {
            registeredBooking.setStatus(BookingStatus.CANCELED);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(registeredBooking));

            assertThatThrownBy(() -> orderService.changeStatus(BOOKING_ID, ChangeStatusDTO.builder().status("CANCELED").build()))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }
    }

    @Nested
    @DisplayName("getUserOrders")
    class GetUserOrders {

        @Test
        @DisplayName("should return all user bookings")
        void allStatuses() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.findAllByUserId(USER_ID, pageable)).thenReturn(new PageImpl<>(List.of(registeredBooking)));
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            Page<BookingDTO> result = orderService.getUserOrders(USERNAME, null, pageable);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by status")
        void filterByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(bookingRepository.findAllByUserIdAndStatus(USER_ID, BookingStatus.REGISTERED, pageable))
                    .thenReturn(new PageImpl<>(List.of(registeredBooking)));
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            Page<BookingDTO> result = orderService.getUserOrders(USERNAME, "REGISTERED", pageable);
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getAllOrders")
    class GetAllOrders {

        @Test
        @DisplayName("should return all bookings")
        void success() {
            Pageable pageable = PageRequest.of(0, 10);
            when(bookingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(registeredBooking)));
            when(bookingMapper.toBookingDTO(any(Booking.class))).thenReturn(bookingDTO);

            Page<BookingDTO> result = orderService.getAllOrders(null, null, null, pageable);
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
