package com.epam.finaltask.service;

import com.epam.finaltask.dto.request.ChangeStatusDTO;
import com.epam.finaltask.dto.response.BookingDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    BookingDTO orderVoucher(UUID voucherId, String username);

    BookingDTO cancelOrder(UUID bookingId, String username);

    BookingDTO changeStatus(UUID bookingId, ChangeStatusDTO request);

    Page<BookingDTO> getUserOrders(String username, String status, Pageable pageable);

    Page<BookingDTO> getAllOrders(String search, String username, String status, Pageable pageable);
}
