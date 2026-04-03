package com.epam.finaltask.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDTO {

    private UUID id;
    private UUID userId;
    private String username;
    private UUID voucherId;
    private String voucherTitle;
    private BigDecimal bookedPrice;
    private String status;
    private LocalDateTime createdAt;
    private String tourType;
    private LocalDate arrivalDate;
    private LocalDate evictionDate;
}
