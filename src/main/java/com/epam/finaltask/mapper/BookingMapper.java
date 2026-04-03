package com.epam.finaltask.mapper;

import com.epam.finaltask.dto.response.BookingDTO;
import com.epam.finaltask.model.Booking;

public interface BookingMapper {
    BookingDTO toBookingDTO(Booking booking);
}
