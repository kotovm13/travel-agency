package com.epam.finaltask.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherDTO {

    private UUID id;
    private String title;
    private String description;
    private Double price;
    private String tourType;
    private String transferType;
    private String hotelType;
    private String status;
    private LocalDate arrivalDate;
    private LocalDate evictionDate;
    private boolean hot;
    private Integer discount;
    private Integer quantity;
    private Long availableQuantity;
}
