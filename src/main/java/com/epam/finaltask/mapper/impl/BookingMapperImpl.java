package com.epam.finaltask.mapper.impl;

import com.epam.finaltask.dto.response.BookingDTO;
import com.epam.finaltask.mapper.BookingMapper;
import com.epam.finaltask.model.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapperImpl implements BookingMapper {

    @Override
    public BookingDTO toBookingDTO(Booking booking) {
        if (booking == null) return null;

        return BookingDTO.builder()
                .id(booking.getId())
                .userId(booking.getUser() != null ? booking.getUser().getId() : null)
                .username(booking.getUser() != null ? booking.getUser().getUsername() : null)
                .voucherId(booking.getVoucher() != null ? booking.getVoucher().getId() : null)
                .voucherTitle(booking.getVoucher() != null ? booking.getVoucher().getTitle() : null)
                .bookedPrice(booking.getBookedPrice())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .createdAt(booking.getCreatedAt())
                .tourType(booking.getVoucher() != null && booking.getVoucher().getTourType() != null
                        ? booking.getVoucher().getTourType().name() : null)
                .arrivalDate(booking.getVoucher() != null ? booking.getVoucher().getArrivalDate() : null)
                .evictionDate(booking.getVoucher() != null ? booking.getVoucher().getEvictionDate() : null)
                .build();
    }
}
