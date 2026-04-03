package com.epam.finaltask.service.impl;

import com.epam.finaltask.dto.request.VoucherCreateDTO;
import com.epam.finaltask.dto.request.VoucherFilterDTO;
import com.epam.finaltask.dto.response.VoucherDTO;
import com.epam.finaltask.exception.VoucherNotFoundException;
import com.epam.finaltask.mapper.VoucherMapper;
import com.epam.finaltask.model.Voucher;
import com.epam.finaltask.model.enums.HotelType;
import com.epam.finaltask.model.enums.TourType;
import com.epam.finaltask.model.enums.TransferType;
import com.epam.finaltask.model.enums.VoucherStatus;
import com.epam.finaltask.repository.BookingRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherServiceImpl")
class VoucherServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VoucherMapper voucherMapper;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private Voucher testVoucher;
    private VoucherDTO testVoucherDTO;
    private VoucherCreateDTO createDTO;
    private static final UUID VOUCHER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        testVoucher = Voucher.builder()
                .id(VOUCHER_ID)
                .title("Test Tour")
                .description("Test description")
                .price(1500.0)
                .tourType(TourType.ADVENTURE)
                .transferType(TransferType.PLANE)
                .hotelType(HotelType.FOUR_STARS)
                .status(VoucherStatus.AVAILABLE)
                .arrivalDate(LocalDate.of(2026, 7, 1))
                .evictionDate(LocalDate.of(2026, 7, 10))
                .isHot(false)
                .discount(0)
                .build();

        testVoucherDTO = VoucherDTO.builder()
                .id(VOUCHER_ID)
                .title("Test Tour")
                .price(1500.0)
                .tourType("ADVENTURE")
                .status("AVAILABLE")
                .build();

        createDTO = VoucherCreateDTO.builder()
                .title("New Tour")
                .description("New description")
                .price(2000.0)
                .tourType("CULTURAL")
                .transferType("TRAIN")
                .hotelType("FIVE_STARS")
                .arrivalDate(LocalDate.of(2026, 8, 1))
                .evictionDate(LocalDate.of(2026, 8, 10))
                .hot(false)
                .discount(0)
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create voucher via mapper")
        void success() {
            when(voucherMapper.toVoucher(createDTO)).thenReturn(testVoucher);
            when(voucherRepository.save(testVoucher)).thenReturn(testVoucher);
            when(voucherMapper.toVoucherDTO(testVoucher)).thenReturn(testVoucherDTO);

            VoucherDTO result = voucherService.create(createDTO);

            assertThat(result).isNotNull();
            verify(voucherMapper).toVoucher(createDTO);
            verify(voucherRepository).save(testVoucher);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update voucher via mapper")
        void success() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(testVoucher));
            when(voucherRepository.save(testVoucher)).thenReturn(testVoucher);
            when(voucherMapper.toVoucherDTO(testVoucher)).thenReturn(testVoucherDTO);

            VoucherDTO result = voucherService.update(VOUCHER_ID, createDTO);

            assertThat(result).isNotNull();
            verify(voucherMapper).updateVoucherFromDTO(createDTO, testVoucher);
        }

        @Test
        @DisplayName("should throw when voucher not found")
        void notFound() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voucherService.update(VOUCHER_ID, createDTO))
                    .isInstanceOf(VoucherNotFoundException.class)
                    .hasMessageContaining(VOUCHER_ID.toString());
        }

        @Test
        @DisplayName("should throw when quantity below active bookings")
        void quantityBelowBookings() {
            createDTO.setQuantity(1);
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(testVoucher));
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(5L);

            assertThatThrownBy(() -> voucherService.update(VOUCHER_ID, createDTO))
                    .isInstanceOf(com.epam.finaltask.exception.InvalidOrderStatusException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing voucher")
        void success() {
            when(voucherRepository.existsById(VOUCHER_ID)).thenReturn(true);

            voucherService.delete(VOUCHER_ID);

            verify(voucherRepository).deleteById(VOUCHER_ID);
        }

        @Test
        @DisplayName("should throw when voucher not found")
        void notFound() {
            when(voucherRepository.existsById(VOUCHER_ID)).thenReturn(false);

            assertThatThrownBy(() -> voucherService.delete(VOUCHER_ID))
                    .isInstanceOf(VoucherNotFoundException.class)
                    .hasMessageContaining(VOUCHER_ID.toString());
        }
    }

    @Nested
    @DisplayName("markHot")
    class MarkHot {

        @Test
        @DisplayName("should set hot status")
        void success() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(testVoucher));
            when(voucherRepository.save(testVoucher)).thenReturn(testVoucher);
            when(voucherMapper.toVoucherDTO(testVoucher)).thenReturn(testVoucherDTO);

            voucherService.markHot(VOUCHER_ID, true);

            assertThat(testVoucher.isHot()).isTrue();
        }
    }

    @Nested
    @DisplayName("setDiscount")
    class SetDiscount {

        @Test
        @DisplayName("should set discount percentage")
        void success() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(testVoucher));
            when(voucherRepository.save(testVoucher)).thenReturn(testVoucher);
            when(voucherMapper.toVoucherDTO(testVoucher)).thenReturn(testVoucherDTO);

            voucherService.setDiscount(VOUCHER_ID, 15);

            assertThat(testVoucher.getDiscount()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return voucher when found")
        void success() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.of(testVoucher));
            when(voucherMapper.toVoucherDTO(testVoucher)).thenReturn(testVoucherDTO);

            VoucherDTO result = voucherService.getById(VOUCHER_ID);

            assertThat(result.getId()).isEqualTo(VOUCHER_ID);
        }

        @Test
        @DisplayName("should throw when not found")
        void notFound() {
            when(voucherRepository.findById(VOUCHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voucherService.getById(VOUCHER_ID))
                    .isInstanceOf(VoucherNotFoundException.class)
                    .hasMessageContaining(VOUCHER_ID.toString());
        }
    }

    @Nested
    @DisplayName("findFiltered")
    class FindFiltered {

        @Test
        @DisplayName("should return all available with empty filter")
        void emptyFilter() {
            Page<Voucher> page = new PageImpl<>(List.of(testVoucher));
            when(voucherRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .thenReturn(page);
            when(voucherMapper.toVoucherDTO(testVoucher)).thenReturn(testVoucherDTO);
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(0L);

            VoucherFilterDTO filter = new VoucherFilterDTO();
            Page<VoucherDTO> result = voucherService.findFiltered(filter, PAGEABLE);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should apply multiple filters")
        void multipleFilters() {
            Page<Voucher> page = new PageImpl<>(List.of(testVoucher));
            when(voucherRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .thenReturn(page);
            when(voucherMapper.toVoucherDTO(testVoucher)).thenReturn(testVoucherDTO);
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(0L);

            VoucherFilterDTO filter = VoucherFilterDTO.builder()
                    .tourType("ADVENTURE")
                    .hotelType("FOUR_STARS")
                    .minPrice(1000.0)
                    .maxPrice(2000.0)
                    .build();

            Page<VoucherDTO> result = voucherService.findFiltered(filter, PAGEABLE);

            assertThat(result.getContent()).hasSize(1);
            verify(voucherRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("should return empty page when no matches")
        void empty() {
            when(voucherRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            VoucherFilterDTO filter = VoucherFilterDTO.builder().search("nonexistent").build();
            Page<VoucherDTO> result = voucherService.findFiltered(filter, PAGEABLE);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should apply price sorting")
        void withSorting() {
            Page<Voucher> page = new PageImpl<>(List.of(testVoucher));
            when(voucherRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .thenReturn(page);
            when(voucherMapper.toVoucherDTO(testVoucher)).thenReturn(testVoucherDTO);
            when(bookingRepository.countActiveBookingsByVoucherId(VOUCHER_ID)).thenReturn(0L);

            VoucherFilterDTO filter = VoucherFilterDTO.builder().sort("price_asc").build();
            Page<VoucherDTO> result = voucherService.findFiltered(filter, PAGEABLE);

            assertThat(result.getContent()).hasSize(1);
        }
    }
}
